package com.corebank.escrita.messaging;

import com.corebank.escrita.domain.TipoTransacao;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contrato do evento publicado no tópico "transacoes-topic". O
 * corebank-consumer-service mantém sua própria cópia deste contrato
 * (e a converte para a entidade Transacao) — é normal em arquitetura
 * de microsserviços cada lado ter sua própria cópia do contrato de evento.
 */
public record TransacaoEvent(
        String idempotencyKey,
        TipoTransacao tipo,
        Long contaId,
        BigDecimal valor,
        String chavePixDestino,
        String cartaoId,
        String estabelecimento,
        Instant criadoEm
) {
}
