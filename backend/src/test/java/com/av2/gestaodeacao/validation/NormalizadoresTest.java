package com.av2.gestaodeacao.validation;

import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormalizadoresTest {

    @Test
    void tickerRemoveEspacosExternosEConverteParaMaiusculas() {
        assertThat(TickerNormalizer.normalizar("  petr4  ")).isEqualTo("PETR4");
        assertThat(TickerNormalizer.normalizar("aapl")).isEqualTo("AAPL");
    }

    @Test
    void tickerRejeitaValorVazioCaracteresDeFormatacaoEComprimentoInvalido() {
        assertThatThrownBy(() -> TickerNormalizer.normalizar("   "))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
        assertThatThrownBy(() -> TickerNormalizer.normalizar("PETR 4"))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
        assertThatThrownBy(() -> TickerNormalizer.normalizar("ABCDEFGHIJKLM"))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
    }

    @Test
    void cnpjAceitaFormatoPontuadoOuSomenteDigitos() {
        assertThat(CnpjNormalizer.normalizar(" 12.345.678/0001-90 "))
                .isEqualTo("12345678000190");
        assertThat(CnpjNormalizer.normalizar("12345678000190"))
                .isEqualTo("12345678000190");
    }

    @Test
    void cnpjRejeitaCaracteresEComprimentosInvalidos() {
        assertThatThrownBy(() -> CnpjNormalizer.normalizar("12.345.678/0001"))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
        assertThatThrownBy(() -> CnpjNormalizer.normalizar("12.345.678/0001-9A"))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
    }

    @Test
    void cepAceitaFormatoPontuadoOuSomenteDigitos() {
        assertThat(CepNormalizer.normalizar(" 12345-678 ")).isEqualTo("12345678");
        assertThat(CepNormalizer.normalizar("12345678")).isEqualTo("12345678");
    }

    @Test
    void cepRejeitaCaracteresEComprimentosInvalidos() {
        assertThatThrownBy(() -> CepNormalizer.normalizar("1234-567"))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
        assertThatThrownBy(() -> CepNormalizer.normalizar("12345-67A"))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
    }
}
