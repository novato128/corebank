package com.corebank.consumer.messaging;

import com.corebank.consumer.domain.Transacao;
import com.corebank.consumer.repository.ContaSaldoRepository;
import com.corebank.consumer.repository.TransacaoRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransacaoPersistenceService {

    private final TransacaoRepository repository;
    private final ContaSaldoRepository contaSaldoRepository;

    @Transactional
    @Retry(name = "bancoEscrita")
    @CircuitBreaker(name = "bancoEscrita", fallbackMethod = "fallbackSalvar")
    public void salvarComProtecao(List<TransacaoEvent> eventos) {
        Set<String> chaves = eventos.stream()
                .map(TransacaoEvent::idempotencyKey)
                .collect(Collectors.toSet());

        Set<String> jaProcessadas = repository.findByIdempotencyKeyIn(chaves.stream().toList())
                .stream()
                .map(Transacao::getIdempotencyKey)
                .collect(Collectors.toSet());

        List<Transacao> novas = eventos.stream()
                .filter(e -> !jaProcessadas.contains(e.idempotencyKey()))
                .map(TransacaoEvent::toEntity)
                .toList();

        if (novas.isEmpty()) {
            return;
        }

        repository.saveAll(novas);

        for (Transacao t : novas) {
            int atualizados = contaSaldoRepository.debitar(t.getContaId(), t.getValor());
            if (atualizados == 0) {
                log.warn("Conta {} não existe na projeção de saldo (conta_saldo); " +
                                "saldo não foi atualizado para a transação {}",
                        t.getContaId(), t.getIdempotencyKey());
            }
        }
    }

    private void fallbackSalvar(List<TransacaoEvent> eventos, Throwable t) {
        log.error("Circuit breaker aberto para escrita no banco, lote de {} transações falhou: {}",
                eventos.size(), t.toString());
        throw new RuntimeException("Banco indisponível para escrita", t);
    }
}
