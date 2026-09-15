package com.av2.gestaodeacao.exceptions;

public class FalhaDeProvedorException extends RuntimeException {
    public FalhaDeProvedorException(String provedor) {
        super("O provedor " + provedor + " retornou dados inválidos ou uma resposta de erro");
    }

    public FalhaDeProvedorException(String provedor, Throwable cause) {
        super("O provedor " + provedor + " retornou dados inválidos ou uma resposta de erro", cause);
    }
}
