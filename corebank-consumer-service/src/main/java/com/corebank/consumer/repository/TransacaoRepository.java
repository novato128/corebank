package com.corebank.consumer.repository;

import com.corebank.consumer.domain.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    List<Transacao> findByIdempotencyKeyIn(List<String> idempotencyKeys);
}
