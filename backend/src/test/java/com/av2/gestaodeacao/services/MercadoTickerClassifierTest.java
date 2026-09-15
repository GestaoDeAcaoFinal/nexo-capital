package com.av2.gestaodeacao.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoTickerClassifierTest {

    private final MercadoTickerClassifier classifier = new MercadoTickerClassifier();

    @Test
    void tickerComQualquerDigitoEhBrasileiro() {
        assertThat(classifier.classificar("PETR4")).isEqualTo("BR");
        assertThat(classifier.classificar("B3SA3")).isEqualTo("BR");
    }

    @Test
    void tickerSomenteComLetrasEhAmericano() {
        assertThat(classifier.classificar("AAPL")).isEqualTo("EUA");
    }
}
