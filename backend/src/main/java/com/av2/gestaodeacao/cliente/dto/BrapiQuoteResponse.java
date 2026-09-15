package com.av2.gestaodeacao.cliente.dto;

import java.math.BigDecimal;

public record BrapiQuoteResponse(
        String longName,
        BigDecimal regularMarketPrice,
        String currency
) {
}
