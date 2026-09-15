package com.av2.gestaodeacao.cliente.dto;

public record ViaCepResponse(
        Boolean erro,
        String logradouro,
        String bairro,
        String localidade,
        String uf
) {
}
