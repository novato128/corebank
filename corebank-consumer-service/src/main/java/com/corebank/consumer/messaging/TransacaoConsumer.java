package com.corebank.consumer.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransacaoConsumer {

    private final TransacaoPersistenceService persistenceService;

    @KafkaListener(topics = "transacoes-topic", groupId = "transacao-persistencia-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void processarLote(List<TransacaoEvent> eventos, Acknowledgment ack) {
        try {
            persistenceService.salvarComProtecao(eventos);
            ack.acknowledge();
            log.info("Lote de {} transações persistido com sucesso", eventos.size());
        } catch (Exception e) {
            log.error("Falha ao persistir lote de {} transações, será reprocessado: {}",
                    eventos.size(), e.getMessage());
            throw e;
        }
    }

}