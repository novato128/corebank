# CoreBank — Documentação técnica

## Contexto

O CoreBank é uma fintech de conta digital com ~2 milhões de contas ativas.
O tráfego se divide em 90% consulta de saldo e 10% escrita (PIX e pagamento
com cartão de crédito), e o Postgres já opera perto do limite de CPU. A
arquitetura foi desenhada para tirar carga síncrona do banco sem violar
duas regras de negócio:

- o saldo exibido pode ter **até 5 segundos de defasagem**;
- a autorização de cartão precisa ser **precisa** (não pode negar por saldo
  errado) — ver seção [Limitação conhecida](#limitação-conhecida-autorização-de-cartão) no final.

## Visão geral dos quatro projetos

O sistema é dividido em quatro aplicações Spring Boot independentes,
cada uma com seu próprio `pom.xml`, ciclo de deploy e capacidade de
escalar sozinha:

```
                         ┌─────────────────────┐
  Cliente ──────────────▶│  corebank-gateway    │  :8080
                         │  (rate limiter +     │
                         │   circuit breaker)   │
                         └──────────┬───────────┘
                     ┌──────────────┴──────────────┐
                     ▼                              ▼
        ┌─────────────────────┐        ┌──────────────────────┐
        │ corebank-saldo-     │        │ corebank-escrita-     │
        │ service :8081       │        │ service :8082         │
        │ (GET saldo)         │        │ (POST pix/cartão)     │
        └──────────┬──────────┘        └───────────┬───────────┘
                    │                               │
             ┌──────┴──────┐                        │ publica
             ▼             ▼                        ▼
          Redis        Postgres              Kafka: transacoes-topic
        (cache-aside,   (miss do                     │
         TTL 5s)        cache)          ┌─────────────┴─────────────┐
             ▲                          ▼                            ▼
             │              ┌───────────────────────┐    (saldo-service também
             └── invalida ──│ corebank-consumer-     │     escuta este tópico,
                 ao          │ service :8083          │     grupo separado, só
                 consumir    │ (persiste no Postgres, │     para invalidar cache)
                 o evento    │  batch + retry + CB)   │
                             └───────────────────────┘
```

Cada seta representa uma chamada de rede real. Não existe nenhum
componente que fale diretamente com "o monólito" — cada serviço só
conhece o Redis, o Kafka ou o Postgres, nunca outro serviço da aplicação
diretamente (a única exceção é o gateway, que é o único que conhece os
endereços dos outros três).

## 1. corebank-gateway — porta 8080

**Responsabilidade única:** ser a porta de entrada e proteger tudo que
vem depois dela.

- É um **Spring Cloud Gateway** (reativo, não confundir com um
  `@RestController` comum). As rotas são declaradas em
  `application.yml`, não em código Java.
- Três rotas: `/api/contas/**` → saldo-service, `/api/pix/**` e
  `/api/pagamentos-cartao/**` → escrita-service.
- Cada rota tem seu próprio **`RequestRateLimiter`** (algoritmo token
  bucket, implementado com um script Lua que roda dentro do Redis — por
  isso o gateway depende do Redis). Os limites de cada rota (1800/s para
  saldo, 120/s + 80/s para escrita) refletem a proporção 90/10 do
  cenário original; ajuste conforme a capacidade real medida.
- O `KeyResolver` (`RateLimiterConfig.java`) particiona o limite **por
  conta** (via header `X-Conta-Id`), não globalmente — assim uma conta
  fazendo muita requisição não consome a cota de outra. Cai para o IP se
  o header não vier.
- Cada rota também tem um filtro **`CircuitBreaker`** (Resilience4j
  reativo): se o serviço de trás cair ou ficar lento além do
  `timeout-duration` configurado, o gateway direciona para
  `FallbackController`, que responde `503` imediatamente em vez de
  deixar o cliente esperando um timeout longo.

Em produção, é neste projeto que normalmente entrariam autenticação
(JWT, OAuth2) e logging centralizado de requisições — não implementados
aqui por não terem sido pedidos, mas é o lugar certo para isso.

## 2. corebank-saldo-service — porta 8081

**Responsabilidade única:** responder `GET /api/contas/{id}/saldo` o
mais rápido possível, sem sobrecarregar o Postgres.

Fluxo de uma requisição (`SaldoController` → `SaldoService`):

1. `@Cacheable(value = "saldos", key = "#contaId")` intercepta a chamada
   **antes** do método executar. Se a chave `saldos::{contaId}` existir
   no Redis, o método `consultarSaldo` nem chega a rodar — o valor
   cacheado volta direto.
2. Em cache miss, o método roda de fato: `ContaSaldoRepository.findById`
   busca no Postgres. Essa chamada está envolvida por
   `@CircuitBreaker(name = "bancoSaldo")` e `@Bulkhead(name =
   "bancoSaldo")` — se o banco estiver falhando ou o circuito estiver
   aberto, cai no `fallbackConsultarSaldo`, que lança uma exceção
   específica (`SaldoIndisponivelException`) tratada pelo
   `GlobalExceptionHandler` como `503`.
3. O resultado é gravado no Redis com **TTL de 5 segundos**
   (`spring.cache.redis.time-to-live` no `application.yml`) — exatamente
   o limite de defasagem aceito pelo negócio. Depois desse tempo, o
   cache expira sozinho e a próxima leitura busca um valor fresco.

**Invalidação ativa do cache:** além do TTL, existe o
`CacheInvalidationListener`, um `@KafkaListener` que escuta o mesmo
tópico `transacoes-topic` publicado pelo escrita-service. Quando uma
transação é publicada, ele chama `SaldoService.invalidarCache(contaId)`
(`@CacheEvict`), removendo a entrada do Redis na hora — não espera o
TTL de 5s se já sabe que o saldo mudou. Isso roda em um **consumer group
próprio** (`saldo-cache-invalidation`), independente do grupo usado pelo
consumer-service para persistir no banco — os dois leem o mesmo tópico
sem interferir um no outro.

Note que este serviço **não tem rate limiter próprio** — essa proteção
já é feita pelo gateway. Aqui só há proteção contra falha do banco
(circuit breaker + bulkhead), que é uma preocupação diferente.

## 3. corebank-escrita-service — porta 8082

**Responsabilidade única:** validar a requisição e publicar um evento no
Kafka. **Este serviço não tem `spring-boot-starter-data-jpa` nem driver
do Postgres no `pom.xml`** — de propósito, para deixar explícito na
árvore de dependências que ele fisicamente não consegue tocar o banco.

Fluxo (`PixController`/`CartaoController` → `TransacaoService`):

1. Bean Validation (`@Valid` + anotações como `@NotNull`,
   `@DecimalMin("0.01")`) rejeita requisições malformadas antes de
   qualquer processamento.
2. `TransacaoService` gera um `idempotencyKey` (UUID) e monta um
   `TransacaoEvent` (record) com os dados da transação.
3. `KafkaTemplate.send(...)` publica no tópico **`transacoes-topic`**,
   usando o `idempotencyKey` como chave da mensagem Kafka. O producer
   está configurado com `acks=all` (só confirma depois que todas as
   réplicas do broker gravaram) e `enable.idempotence=true` (evita que
   um retry automático do próprio producer duplique a mensagem).
4. O controller responde **`202 Accepted`** imediatamente, com o
   `idempotencyKey` no header `X-Request-Id` — o cliente pode usar esse
   valor para rastrear a transação depois, mas o `202` não significa que
   ela já está persistida no banco, só que foi aceita para
   processamento assíncrono.

Não há circuit breaker/retry neste serviço porque ele não faz nenhuma
chamada bloqueante a um recurso instável — só publica no Kafka, que já
tem seu próprio mecanismo de retry no nível do producer.

## 4. corebank-consumer-service — porta 8083

**Responsabilidade única:** ser o único componente do sistema que
escreve na tabela `transacao` do Postgres. Não expõe nenhum endpoint de
negócio (só actuator).

Fluxo (`TransacaoConsumer.processarLote`):

1. `@KafkaListener` configurado em modo **batch**
   (`spring.kafka.listener.type: batch`,
   `max.poll.records: 100`) — em vez de processar uma mensagem por vez,
   recebe até 100 de uma vez em uma `List<TransacaoEvent>`.
2. `salvarComProtecao` primeiro consulta quais `idempotencyKey` do lote
   **já existem** no banco (`findByIdempotencyKeyIn`) e filtra os que
   já foram processados — isso é o que torna o reprocessamento seguro:
   se o Kafka reentregar o mesmo lote (porque o processo caiu antes do
   ack, por exemplo), as transações já persistidas são ignoradas na
   segunda tentativa, e só as novas são inseridas.
3. `repository.saveAll(novas)` grava tudo de uma vez — com
   `hibernate.jdbc.batch_size: 50` configurado, o Hibernate agrupa os
   inserts em poucas idas ao banco em vez de uma conexão por registro.
4. Esse método inteiro está anotado com `@Retry(name = "bancoEscrita")`
   e `@CircuitBreaker(name = "bancoEscrita")`: se o Postgres falhar
   momentaneamente, tenta de novo com backoff exponencial (500ms, 1s,
   2s); se continuar falhando, o circuito abre e cai no
   `fallbackSalvar`, que relança a exceção.
5. O listener está em `ack-mode: manual_immediate` — o
   `Acknowledgment.acknowledge()` só é chamado **depois** que
   `salvarComProtecao` retorna com sucesso. Se lançar exceção, o offset
   não é confirmado.
6. Quando a exceção sobe até o container do Kafka, o
   `DefaultErrorHandler` (configurado em `KafkaConfig`) entra em ação:
   tenta reprocessar o lote com backoff exponencial por até ~10
   segundos; se ainda assim falhar, publica o lote inteiro no tópico
   **`transacoes-topic.DLT`** (dead letter) em vez de travar o consumer
   indefinidamente ou perder a informação silenciosamente.

## Kafka: um tópico, dois consumer groups

Só existe um tópico de negócio, `transacoes-topic`, usado tanto por PIX
quanto por pagamento com cartão (o campo `tipo` no evento diferencia os
dois). Dois consumer groups independentes leem essa mesma sequência de
eventos com propósitos diferentes:

| Consumer group | Serviço | O que faz com o evento |
|---|---|---|
| `transacao-persistencia-group` | consumer-service | Persiste a transação no Postgres |
| `saldo-cache-invalidation` | saldo-service | Só invalida a chave de cache da conta afetada |

Isso é uma vantagem estrutural do Kafka sobre uma fila tradicional
(RabbitMQ, SQS): a mesma mensagem pode ser consumida integralmente por
múltiplos assinantes independentes, sem que um "roube" a mensagem do
outro nem que o produtor precise saber quem vai consumir.

## Idempotência, ponta a ponta

O `idempotencyKey` é gerado uma única vez, no escrita-service, e viaja
com o evento até o consumer-service, que o usa como chave única
(`@Column(unique = true)`) na tabela `transacao`. Isso protege contra
duplicação em três pontos diferentes da cadeia:

- o producer Kafka pode reenviar a mesma mensagem em caso de falha de
  rede (`enable.idempotence=true` evita duplicar *no nível do broker*,
  mas isso sozinho não cobre um app-level retry);
- o Kafka pode reentregar o mesmo lote ao consumer se o ack não tiver
  sido confirmado a tempo (`at-least-once delivery`, o padrão do Kafka);
- o próprio `DefaultErrorHandler` pode tentar reprocessar o lote.

Em todos os casos, `findByIdempotencyKeyIn` + filtro antes do
`saveAll` garante que a mesma transação nunca é gravada duas vezes.

## Onde cada padrão de resiliência está aplicado

| Padrão | Onde | Protege contra |
|---|---|---|
| Rate limiter | gateway (por rota) | Pico de tráfego sobrecarregando os serviços de trás |
| Circuit breaker | gateway, saldo-service, consumer-service | Efeito cascata quando um recurso downstream (serviço ou banco) está degradado |
| Bulkhead | saldo-service | Todas as threads da aplicação ficarem presas esperando o banco |
| Retry com backoff exponencial | consumer-service | Falhas transitórias e curtas do Postgres |
| Cache-aside + TTL | saldo-service | Tráfego de leitura repetido batendo no banco |
| Batch insert + idempotência | consumer-service | Muitas idas ao banco e duplicação em reprocessamento |
| Producer idempotente + acks=all | escrita-service | Perda ou duplicação de mensagem entre o serviço e o Kafka |
| DLQ (dead letter topic) | consumer-service | Mensagem "envenenada" travando o consumer para sempre |

## Modelo de dados

O saldo-service e o consumer-service **não compartilham a mesma
entidade JPA**, mesmo apontando para o mesmo banco `corebank`:

- `ContaSaldo` (saldo-service): projeção de leitura, só `contaId`,
  `saldo`, `atualizadoEm`. Em uma arquitetura mais madura, essa tabela
  seria alimentada por CDC/replicação a partir do ledger, não editada
  diretamente.
- `Transacao` (consumer-service): o registro transacional de cada PIX
  ou pagamento, com todos os campos específicos de cada tipo.

Isso é intencional: cada serviço só enxerga o formato de dado que
precisa para seu próprio trabalho, e pode evoluir seu schema sem
depender do outro.

## Limitação conhecida: autorização de cartão

O fluxo de escrita implementado aqui é **assíncrono** para os dois
tipos de transação — adequado para o requisito de saldo com até 5s de
defasagem. Na prática, autorização de cartão de crédito em produção
normalmente precisa de uma resposta síncrona e exata (aprovar/negar na
hora, sem arriscar aprovar algo que o saldo real não cobre). O padrão
para isso é fazer uma checagem atômica de limite direto no Redis (script
Lua) *antes* de publicar o evento, mantendo o Kafka só para persistir o
histórico. Essa camada não está implementada nos projetos atuais.
