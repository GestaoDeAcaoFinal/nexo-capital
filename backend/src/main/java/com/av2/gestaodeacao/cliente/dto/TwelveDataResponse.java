package com.av2.gestaodeacao.cliente.dto;

import java.math.BigDecimal;

public record TwelveDataResponse(
        Integer code,
        String status,
        String message,
        String name,
        BigDecimal close,
        String currency
) {
}
