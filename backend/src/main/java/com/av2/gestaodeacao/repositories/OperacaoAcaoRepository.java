package com.av2.gestaodeacao.repositories;

import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.OperacaoAcao;
import com.av2.gestaodeacao.domains.enums.TipoOperacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperacaoAcaoRepository extends JpaRepository<OperacaoAcao, Long> {
    List<OperacaoAcao> findByAcaoAndTipoOperacao(Acao acao,
                                                 TipoOperacao tipoOperacao);

    List<OperacaoAcao> findByAcao(Acao acao);

    List<OperacaoAcao> findByTipoOperacao(TipoOperacao tipoOperacao);
}
