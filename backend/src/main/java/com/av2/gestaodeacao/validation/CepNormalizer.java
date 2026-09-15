package com.av2.gestaodeacao.validation;

import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;

public final class CepNormalizer {

    private CepNormalizer() {
    }

    public static String normalizar(String cep) {
        if (cep == null) {
            throw new EntradaOuOperacaoInvalidaException("CEP é obrigatório");
        }

        String valor = cep.trim();
        if (!valor.matches("[0-9-]+")) {
            throw new EntradaOuOperacaoInvalidaException("CEP possui caracteres inválidos");
        }

        String normalizado = valor.replace("-", "");
        if (!normalizado.matches("\\d{8}")) {
            throw new EntradaOuOperacaoInvalidaException("CEP deve conter 8 dígitos");
        }
        return normalizado;
    }
}
