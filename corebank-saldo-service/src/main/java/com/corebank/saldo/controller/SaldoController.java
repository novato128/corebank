package com.corebank.saldo.controller;

import com.corebank.saldo.dto.SaldoResponse;
import com.corebank.saldo.service.SaldoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contas")
@RequiredArgsConstructor
public class SaldoController {

    private final SaldoService saldoService;

    @GetMapping("/{contaId}/saldo")
    public ResponseEntity<SaldoResponse> consultarSaldo(@PathVariable Long contaId) {
        return ResponseEntity.ok(saldoService.consultarSaldo(contaId));
    }
}
