package com.av2.gestaodeacao.services;

import org.springframework.stereotype.Component;

@Component
public class MercadoTickerClassifier {

    public String classificar(String ticker) {
        return ticker.chars().anyMatch(Character::isDigit) ? "BR" : "EUA";
    }
}
