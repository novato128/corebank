package com.corebank.saldo.messaging;

import com.corebank.saldo.service.SaldoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Fecha o loop de consistência do cache: quando o corebank-escrita-service
 * confirma um PIX ou pagamento com cartão, publica no tópico "transacoes-topic".
 * Aqui só evictamos a chave — o próximo GET busca o valor fresco no banco
 * e recacheia. Mais simples e menos propenso a erro do que tentar atualizar
 * o valor em cache diretamente.
 */
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
