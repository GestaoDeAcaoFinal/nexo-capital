package com.av2.gestaodeacao.validation;

import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;

public final class CnpjNormalizer {

    private CnpjNormalizer() {
    }

    public static String normalizar(String cnpj) {
        if (cnpj == null) {
            throw new EntradaOuOperacaoInvalidaException("CNPJ é obrigatório");
        }

        String valor = cnpj.trim();
        if (!valor.matches("[0-9./-]+")) {
            throw new EntradaOuOperacaoInvalidaException("CNPJ possui caracteres inválidos");
        }

        String normalizado = valor.replaceAll("[./-]", "");
        if (!normalizado.matches("\\d{14}")) {
            throw new EntradaOuOperacaoInvalidaException("CNPJ deve conter 14 dígitos");
        }
        return normalizado;
    }
}
