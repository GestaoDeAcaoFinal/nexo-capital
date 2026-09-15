package com.av2.gestaodeacao.cliente.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BrasilApiCnpjResponse(
        @JsonProperty("razao_social") String razaoSocial,
        @JsonProperty("nome_fantasia") String nomeFantasia,
        String email,
        @JsonProperty("ddd_telefone_1") String telefone,
        @JsonProperty("descricao_situacao_cadastral") String situacaoCadastral
) {
}
