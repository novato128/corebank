package com.corebank.saldo.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransacaoConcluidaEvent(Long contaId) {
}
