package com.corebank.consumer.repository;

import com.corebank.consumer.domain.ContaSaldo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ContaSaldoRepository extends JpaRepository<ContaSaldo, Long> {

    @Modifying
    @Query("UPDATE ContaSaldo c SET c.saldo = c.saldo - :valor, c.atualizadoEm = CURRENT_TIMESTAMP WHERE c.contaId = :contaId")
    int debitar(@Param("contaId") Long contaId, @Param("valor") BigDecimal valor);
}

