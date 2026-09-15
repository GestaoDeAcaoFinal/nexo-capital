package com.av2.gestaodeacao.domains.dtos;

public record PosicaoCarteiraDTO(
        Long acaoId,
        String ticker,
        Integer quantidadeAtual,
        Double precoMedio,
        Double custoTotal,
        Double cotacaoAtual,
        Double valorAtual
) {
}
