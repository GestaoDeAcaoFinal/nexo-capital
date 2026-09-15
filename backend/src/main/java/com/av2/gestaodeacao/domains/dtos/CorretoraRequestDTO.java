package com.av2.gestaodeacao.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CorretoraRequestDTO {

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(
            regexp = "^\\s*(?:\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})\\s*$",
            message = "CNPJ deve conter 14 dígitos, com ou sem pontuação"
    )
    private String cnpj;

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(
            regexp = "^\\s*(?:\\d{8}|\\d{5}-\\d{3})\\s*$",
            message = "CEP deve conter 8 dígitos, com ou sem hífen"
    )
    private String cep;

}
