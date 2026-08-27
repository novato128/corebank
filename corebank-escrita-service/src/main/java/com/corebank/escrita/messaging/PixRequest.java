package com.corebank.escrita.messaging;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PixRequest(
        @NotNull Long contaOrigemId,
        @NotBlank String chavePixDestino,
        @NotNull @DecimalMin("0.01") BigDecimal valor
) {
}
