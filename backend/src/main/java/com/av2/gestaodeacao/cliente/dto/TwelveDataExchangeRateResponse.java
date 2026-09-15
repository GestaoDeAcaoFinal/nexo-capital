package com.av2.gestaodeacao.cliente.dto;

import java.math.BigDecimal;

public record TwelveDataExchangeRateResponse(
        Integer code,
        String status,
        String message,
        String symbol,
        BigDecimal rate,
        Long timestamp
) {
}
