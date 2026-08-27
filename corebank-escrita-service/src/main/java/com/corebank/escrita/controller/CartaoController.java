package com.corebank.escrita.controller;

import com.corebank.escrita.messaging.PagamentoCartaoRequest;
import com.corebank.escrita.service.TransacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pagamentos-cartao")
@RequiredArgsConstructor
public class CartaoController {

    private final TransacaoService transacaoService;

    @PostMapping
    public ResponseEntity<Void> pagar(@RequestBody @Valid PagamentoCartaoRequest request) {
        String idempotencyKey = transacaoService.publicarPagamentoCartao(request);

        return ResponseEntity.accepted()
                .header("X-Request-Id", idempotencyKey)
                .build();
    }
}
