package com.av2.gestaodeacao.domains.dtos;

import java.util.List;

public record ResumoCarteiraDTO(
        List<PosicaoCarteiraDTO> posicoes,
        Integer quantidadeTotal,
        Double precoMedioCarteira,
        Double custoTotalCarteira,
        Double valorAtualCarteira
) {
}
