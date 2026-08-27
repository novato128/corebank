package com.corebank.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Chamado pelo filtro CircuitBreaker do Gateway quando o serviço de
 * trás está fora do ar ou o circuito está aberto. Responde rápido em
 * vez de deixar o cliente esperando um timeout longo.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/saldo")
    public ResponseEntity<Map<String, Object>> fallbackSaldo() {
        return corpo("Consulta de saldo temporariamente indisponível, tente novamente em instantes");
    }

    @GetMapping("/escrita")
    public ResponseEntity<Map<String, Object>> fallbackEscrita() {
        return corpo("Serviço de transações temporariamente indisponível, tente novamente em instantes");
    }

    private ResponseEntity<Map<String, Object>> corpo(String mensagem) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("timestamp", Instant.now().toString(), "mensagem", mensagem));
    }
}
