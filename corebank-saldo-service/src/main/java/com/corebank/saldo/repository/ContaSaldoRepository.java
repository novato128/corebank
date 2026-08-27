package com.corebank.saldo.repository;

import com.corebank.saldo.domain.ContaSaldo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContaSaldoRepository extends JpaRepository<ContaSaldo, Long> {
}
