package com.corebank.escrita.service;

import com.corebank.escrita.domain.TipoTransacao;
import com.corebank.escrita.messaging.PagamentoCartaoRequest;
import com.corebank.escrita.messaging.PixRequest;
import com.corebank.escrita.messaging.TransacaoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Nunca escreve direto no banco: publica o evento no Kafka (acks=all
 * garante durabilidade) e responde. O TransacaoConsumer é quem persiste,
 * em lote, no ritmo que o Postgres aguenta.
 *
 * IMPORTANTE: este fluxo assíncrono cobre bem o PIX, onde até 5s de
 * defasagem no saldo são aceitáveis. Autorização de cartão de crédito em
 * produção normalmente exige uma checagem SÍNCRONA de saldo/limite antes
 * de aprovar (não pode autorizar e descobrir depois que não havia saldo).
 * Se precisar disso, o padrão é validar o limite via uma operação atômica
 * em Redis (script Lua) antes de publicar o evento — posso implementar
 * isso separadamente se fizer sentido para o seu caso.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransacaoService {

    private static final String TOPICO = "transacoes-topic";

    private final KafkaTemplate<String, TransacaoEvent> kafkaTemplate;

    public String publicarPix(PixRequest request) {
        String idempotencyKey = UUID.randomUUID().toString();

        TransacaoEvent event = new TransacaoEvent(
                idempotencyKey,
                TipoTransacao.PIX,
                request.contaOrigemId(),
                request.valor(),
                request.chavePixDestino(),
                null,
                null,
                Instant.now()
        );

        publicar(idempotencyKey, event);
        return idempotencyKey;
    }

    public String publicarPagamentoCartao(PagamentoCartaoRequest request) {
        String idempotencyKey = UUID.randomUUID().toString();

        TransacaoEvent event = new TransacaoEvent(
                idempotencyKey,
                TipoTransacao.CARTAO_CREDITO,
                request.contaId(),
                request.valor(),
                null,
                request.cartaoId(),
                request.estabelecimento(),
                Instant.now()
        );

        publicar(idempotencyKey, event);
        return idempotencyKey;
    }

    private void publicar(String idempotencyKey, TransacaoEvent event) {
        kafkaTemplate.send(TOPICO, idempotencyKey, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar transação {}: {}", idempotencyKey, ex.getMessage());
                    } else {
                        log.debug("Transação {} ({}) publicada no tópico {}",
                                idempotencyKey, event.tipo(), TOPICO);
                    }
                });
    }
}
