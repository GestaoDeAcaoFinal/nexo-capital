package com.av2.gestaodeacao;

import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.repositories.AcaoRepository;
import com.av2.gestaodeacao.repositories.CorretoraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "TWELVE_DATA_API_KEY=test-key",
        "DDL_AUTO=validate"
})
class PersistenceSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AcaoRepository acaoRepository;

    @Autowired
    private CorretoraRepository corretoraRepository;

    @BeforeEach
    void limparDados() {
        acaoRepository.deleteAll();
        corretoraRepository.deleteAll();
    }

    @Test
    void schemaUsaAcoesComUnicidadeEChavesEstrangeirasNomeadas() {
        assertThat(tabelasChamadas("ACOES")).isOne();
        assertThat(tabelasChamadas("PRODUTO")).isZero();

        assertThat(constraintsChamadas("ACOES", "UK_ACOES_TICKER", "UNIQUE")).isOne();
        assertThat(constraintsChamadas("CORRETORA", "UK_CORRETORA_CNPJ", "UNIQUE")).isOne();
        assertThat(constraintsChamadas("ACOES", "FK_ACOES_CORRETORA", "FOREIGN KEY")).isOne();
        assertThat(constraintsChamadas(
                "OPERACAO_ACAO", "FK_OPERACAO_ACAO_ACAO", "FOREIGN KEY")).isOne();
    }

    @Test
    void segundaAcaoComMesmoTickerEhRejeitadaPeloBanco() {
        Acao primeira = new Acao();
        primeira.setTicker("PETR4");
        acaoRepository.saveAndFlush(primeira);

        Acao duplicada = new Acao();
        duplicada.setTicker("PETR4");

        assertThatThrownBy(() -> acaoRepository.saveAndFlush(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void segundaCorretoraComMesmoCnpjEhRejeitadaPeloBanco() {
        Corretora primeira = new Corretora();
        primeira.setCnpj("12345678000190");
        corretoraRepository.saveAndFlush(primeira);

        Corretora duplicada = new Corretora();
        duplicada.setCnpj("12345678000190");

        assertThatThrownBy(() -> corretoraRepository.saveAndFlush(duplicada))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long tabelasChamadas(String tabela) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM INFORMATION_SCHEMA.TABLES
                 WHERE TABLE_SCHEMA = 'PUBLIC'
                   AND TABLE_NAME = ?
                """, Long.class, tabela);
    }

    private long constraintsChamadas(String tabela, String constraint, String tipo) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                 WHERE CONSTRAINT_SCHEMA = 'PUBLIC'
                   AND TABLE_NAME = ?
                   AND CONSTRAINT_NAME = ?
                   AND CONSTRAINT_TYPE = ?
                """, Long.class, tabela, constraint, tipo);
    }
}
