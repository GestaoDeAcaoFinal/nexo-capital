package com.av2.gestaodeacao.domains.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AcaoRequestDTO {

    @NotBlank(message = "Ticker é obrigatório")
    @Pattern(
            regexp = "^\\s*(?=.*[A-Za-z])[A-Za-z0-9]{1,12}\\s*$",
            message = "Ticker deve conter de 1 a 12 letras ou números"
    )
    private String ticker;

    @NotNull(message = "Corretora é obrigatória")
    @Positive(message = "Corretora deve possuir identificador positivo")
    private Long corretoraId;
}
