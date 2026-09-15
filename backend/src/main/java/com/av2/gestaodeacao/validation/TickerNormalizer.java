package com.av2.gestaodeacao.validation;

import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;

import java.util.Locale;

public final class TickerNormalizer {

    private static final String FORMATO_TICKER = "(?=.*[A-Z])[A-Z0-9]{1,12}";

    private TickerNormalizer() {
    }

    public static String normalizar(String ticker) {
        if (ticker == null) {
            throw new EntradaOuOperacaoInvalidaException("Ticker é obrigatório");
        }

        String normalizado = ticker.trim().toUpperCase(Locale.ROOT);
        if (!normalizado.matches(FORMATO_TICKER)) {
            throw new EntradaOuOperacaoInvalidaException("Ticker deve conter de 1 a 12 letras ou números");
        }
        return normalizado;
    }
}
