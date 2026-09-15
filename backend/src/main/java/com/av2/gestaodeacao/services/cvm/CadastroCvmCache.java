package com.av2.gestaodeacao.services.cvm;

import com.av2.gestaodeacao.cliente.cvm.CadastroCvmGateway;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import com.av2.gestaodeacao.validation.CnpjNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
public class CadastroCvmCache {

    private final CadastroCvmGateway gateway;
    private final CadastroCvmParser parser;
    private final Duration ttl;
    private final Clock clock;
    private volatile Snapshot snapshot;

    @Autowired
    public CadastroCvmCache(
            CadastroCvmGateway gateway,
            CadastroCvmParser parser,
            @Value("${integracoes.cvm.cache-ttl:24h}") Duration ttl
    ) {
        this(gateway, parser, ttl, Clock.systemUTC());
    }

    CadastroCvmCache(
            CadastroCvmGateway gateway,
            CadastroCvmParser parser,
            Duration ttl,
            Clock clock
    ) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("O TTL do cadastro CVM deve ser positivo");
        }
        this.gateway = gateway;
        this.parser = parser;
        this.ttl = ttl;
        this.clock = clock;
    }

    public boolean intermediarioAtivo(String cnpj) {
        return intermediariosAtivos().contains(CnpjNormalizer.normalizar(cnpj));
    }

    public Set<String> intermediariosAtivos() {
        Instant agora = clock.instant();
        Snapshot atual = snapshot;
        if (valido(atual, agora)) {
            return atual.cnpjs();
        }
        return atualizar(agora);
    }

    private synchronized Set<String> atualizar(Instant agora) {
        Snapshot atual = snapshot;
        if (valido(atual, agora)) {
            return atual.cnpjs();
        }
        try {
            Set<String> novosCnpjs = Set.copyOf(parser.parse(gateway.baixarCadastro()));
            Snapshot novo = new Snapshot(novosCnpjs, agora);
            snapshot = novo;
            return novo.cnpjs();
        } catch (RuntimeException e) {
            throw new ProvedorIndisponivelException("CVM", e);
        }
    }

    private boolean valido(Snapshot candidato, Instant agora) {
        return candidato != null && agora.isBefore(candidato.carregadoEm().plus(ttl));
    }

    private record Snapshot(Set<String> cnpjs, Instant carregadoEm) {
    }
}
