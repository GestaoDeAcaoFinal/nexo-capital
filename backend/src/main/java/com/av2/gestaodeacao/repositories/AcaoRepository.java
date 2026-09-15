package com.av2.gestaodeacao.repositories;

import com.av2.gestaodeacao.domains.Acao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

    Optional<Acao> findByTicker(String ticker);
}
