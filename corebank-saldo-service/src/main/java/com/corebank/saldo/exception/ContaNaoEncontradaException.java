package com.corebank.saldo.exception;

public class ContaNaoEncontradaException extends RuntimeException {

    public ContaNaoEncontradaException(Long contaId) {
        super("Conta não encontrada: " + contaId);
    }
}
