package com.corebank.consumer.messaging;

import com.corebank.consumer.domain.TipoTransacao;
import com.corebank.consumer.domain.Transacao;

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
    public Transacao toEntity() {
        Transacao transacao = new Transacao();
        transacao.setIdempotencyKey(idempotencyKey);
        transacao.setTipo(tipo);
        transacao.setContaId(contaId);
        transacao.setValor(valor);
        transacao.setChavePixDestino(chavePixDestino);
        transacao.setCartaoId(cartaoId);
        transacao.setEstabelecimento(estabelecimento);
        transacao.setCriadoEm(criadoEm);
        return transacao;
    }
}
