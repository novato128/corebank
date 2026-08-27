package com.corebank.saldo.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Só nos interessa o contaId para invalidar o cache. Os demais campos
 * publicados pelo corebank-escrita-service (valor, tipo, etc.) são ignorados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TransacaoConcluidaEvent(Long contaId) {
}
