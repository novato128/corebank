package com.corebank.escrita.messaging;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PagamentoCartaoRequest(
        @NotNull Long contaId,
        @NotBlank String cartaoId,
        @NotBlank String estabelecimento,
        @NotNull @DecimalMin("0.01") BigDecimal valor
) {
}
