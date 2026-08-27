package com.corebank.saldo.messaging;

import com.corebank.saldo.service.SaldoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener {

    private final SaldoService saldoService;

    @KafkaListener(topics = "transacoes-topic", groupId = "saldo-cache-invalidation")
    public void aoConcluirTransacao(TransacaoConcluidaEvent evento) {
        log.debug("Invalidando cache de saldo para conta {}", evento.contaId());
        saldoService.invalidarCache(evento.contaId());
    }
}
