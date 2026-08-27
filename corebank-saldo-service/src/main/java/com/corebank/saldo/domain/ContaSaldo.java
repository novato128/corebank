package com.corebank.saldo.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContaSaldo implements Serializable {

    @Id
    private Long contaId;

    private BigDecimal saldo;

    private Instant atualizadoEm;
}
