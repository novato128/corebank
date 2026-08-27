package com.corebank.saldo.service;

import com.corebank.saldo.domain.ContaSaldo;
import com.corebank.saldo.dto.SaldoResponse;
import com.corebank.saldo.exception.ContaNaoEncontradaException;
import com.corebank.saldo.repository.ContaSaldoRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * O rate limiting da entrada fica no corebank-gateway. Aqui só protegemos
 * o banco: cache-aside no Redis (TTL 5s) e, em cache miss, circuit breaker
 * + bulkhead ao redor da chamada ao Postgres.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SaldoService {

    private final ContaSaldoRepository repository;

    @Cacheable(value = "saldos", key = "#contaId")
    @CircuitBreaker(name = "bancoSaldo", fallbackMethod = "fallbackConsultarSaldo")
    @Bulkhead(name = "bancoSaldo")
    public SaldoResponse consultarSaldo(Long contaId) {
        log.debug("Cache miss para saldo da conta {}, indo ao banco", contaId);
        ContaSaldo conta = repository.findById(contaId)
                .orElseThrow(() -> new ContaNaoEncontradaException(contaId));
        return new SaldoResponse(conta.getContaId(), conta.getSaldo(), conta.getAtualizadoEm());
    }

    private SaldoResponse fallbackConsultarSaldo(Long contaId, Throwable t) {
        log.warn("Fallback acionado para saldo da conta {}: {}", contaId, t.toString());
        throw new SaldoIndisponivelException(
                "Consulta de saldo temporariamente indisponível, tente novamente em instantes");
    }

    @CacheEvict(value = "saldos", key = "#contaId")
    public void invalidarCache(Long contaId) {
        log.info("Cache de saldo invalidado para conta {}", contaId);
    }

    public static class SaldoIndisponivelException extends RuntimeException {
        public SaldoIndisponivelException(String message) {
            super(message);
        }
    }
}
