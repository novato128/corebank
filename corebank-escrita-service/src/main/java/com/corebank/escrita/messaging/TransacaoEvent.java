package com.corebank.escrita.messaging;

import com.corebank.escrita.domain.TipoTransacao;

import java.math.BigDecimal;
import java.time.Instant;

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
