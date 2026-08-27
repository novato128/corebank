package com.corebank.saldo.dto;

import java.math.BigDecimal;
import java.time.Instant;

//public record SaldoResponse(
//        Long contaId,
//        BigDecimal saldo,
//        Instant atualizadoEm
//) {
//}
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaldoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long contaId;
    private BigDecimal saldo;
    private Instant atualizadoEm;
}
