package com.av2.gestaodeacao.repositories;

import com.av2.gestaodeacao.domains.Corretora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorretoraRepository extends JpaRepository<Corretora, Long> {

    Optional<Corretora> findByCnpj(String cnpj);
}
