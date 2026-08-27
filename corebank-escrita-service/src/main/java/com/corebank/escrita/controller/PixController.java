package com.corebank.escrita.controller;

import com.corebank.escrita.messaging.PixRequest;
import com.corebank.escrita.service.TransacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pix")
@RequiredArgsConstructor
public class PixController {

    private final TransacaoService transacaoService;

    @PostMapping
    public ResponseEntity<Void> transferir(@RequestBody @Valid PixRequest request) {
        String idempotencyKey = transacaoService.publicarPix(request);

        // 202: aceito para processamento assíncrono, ainda não confirmado no banco
        return ResponseEntity.accepted()
                .header("X-Request-Id", idempotencyKey)
                .build();
    }
}
