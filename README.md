# CoreBank — arquitetura em 4 projetos

Cada responsabilidade é um Spring Boot separado, cada um com seu próprio
`pom.xml` e podendo ser implantado e escalado de forma independente.

| Projeto | Porta | Responsabilidade |
|---|---|---|
| `corebank-gateway` | 8080 | Ponto de entrada único. Rate limiting (Redis) e circuit breaker por rota. |
| `corebank-saldo-service` | 8081 | `GET /api/contas/{id}/saldo`. Cache-aside no Redis (TTL 5s) + circuit breaker no banco. |
| `corebank-escrita-service` | 8082 | `POST /api/pix` e `POST /api/pagamentos-cartao`. Só publica no Kafka, nunca toca o banco. |
| `corebank-consumer-service` | 8083 | Sem endpoint REST de negócio. Consome `transacoes-topic` em lote e persiste no Postgres com idempotência, retry e circuit breaker. |


Dois consumer groups diferentes lêem o mesmo tópico de forma independente:
`transacao-persistencia-group` (no consumer-service, grava no banco) e
`saldo-cache-invalidation` (no saldo-service, só invalida a chave de cache
da conta).

## Como rodar

1. Suba a infraestrutura compartilhada (na raiz deste diretório):
   ```bash
   docker compose up -d
   ```

2. Rode cada projeto em um terminal separado:
   ```bash
   cd corebank-gateway && ./mvnw spring-boot:run
   cd corebank-saldo-service && ./mvnw spring-boot:run
   cd corebank-escrita-service && ./mvnw spring-boot:run
   cd corebank-consumer-service && ./mvnw spring-boot:run
   ```

3. Teste pelo gateway:
   ```bash
   # consulta de saldo
   curl http://localhost:8080/api/contas/1/saldo

   # PIX
   curl -X POST http://localhost:8080/api/pix \
     -H "Content-Type: application/json" \
     -d '{"contaOrigemId": 1, "chavePixDestino": "fulano@banco.com", "valor": 150.00}'

   # pagamento com cartão
   curl -X POST http://localhost:8080/api/pagamentos-cartao \
     -H "Content-Type: application/json" \
     -d '{"contaId": 1, "cartaoId": "1234-5678", "estabelecimento": "Mercado XPTO", "valor": 89.90}'
   ```

## Trade-off: autorização de cartão de crédito

O fluxo de escrita aqui é assíncrono (event-driven) — 
Isso é adequado quando até alguns segundos de defasagem no saldo
são toleráveis. Autorização de cartão em produção geralmente exige uma
checagem **síncrona** de limite antes de aprovar a compra (não dá para
autorizar e só depois descobrir que não havia saldo).