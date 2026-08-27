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
