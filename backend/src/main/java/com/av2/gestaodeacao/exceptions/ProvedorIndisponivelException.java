package com.av2.gestaodeacao.exceptions;

public class ProvedorIndisponivelException extends RuntimeException {
    public ProvedorIndisponivelException(String provedor) {
        super("O provedor " + provedor + " está indisponível");
    }

    public ProvedorIndisponivelException(String provedor, Throwable cause) {
        super("O provedor " + provedor + " está indisponível", cause);
    }
}
