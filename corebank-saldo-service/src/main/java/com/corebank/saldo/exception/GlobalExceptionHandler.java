package com.corebank.saldo.exception;

import com.corebank.saldo.service.SaldoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ContaNaoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrada(ContaNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(corpoErro(ex.getMessage()));
    }

    @ExceptionHandler(SaldoService.SaldoIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleIndisponivel(SaldoService.SaldoIndisponivelException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpoErro(ex.getMessage()));
    }

    private Map<String, Object> corpoErro(String mensagem) {
        return Map.of("timestamp", Instant.now().toString(), "mensagem", mensagem);
    }
}
