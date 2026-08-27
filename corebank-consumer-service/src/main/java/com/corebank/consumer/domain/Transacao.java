package com.corebank.consumer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private TipoTransacao tipo;

    private Long contaId;

    private BigDecimal valor;

    // usado apenas em transações PIX
    private String chavePixDestino;

    // usados apenas em pagamento com cartão
    private String cartaoId;
    private String estabelecimento;

    private Instant criadoEm;
}
