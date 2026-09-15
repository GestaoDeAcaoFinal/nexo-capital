package com.av2.gestaodeacao;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class LegacySchemaMigrationTest {

    @Test
    void migraSchemaLegadoPreservandoDadosEChaves() throws Exception {
        DataSource dataSource = bancoLegado();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO corretora (id, cnpj) VALUES (1, '12345678000190')");
        jdbc.update("INSERT INTO produto (id, ticker, corretora_id) VALUES (1, 'PETR4', 1)");
        jdbc.update("INSERT INTO operacao_acao (id, acao_id) VALUES (1, 1)");

        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.xml");
        liquibase.afterPropertiesSet();

        assertThat(contar(jdbc, "SELECT COUNT(*) FROM acoes")).isOne();
        assertThat(contar(jdbc, "SELECT COUNT(*) FROM operacao_acao o "
                + "JOIN acoes a ON a.id = o.acao_id WHERE a.ticker = 'PETR4'"))
                .isOne();
        assertThat(contar(jdbc, "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'PRODUTO'"))
                .isZero();
    }

    private DataSource bancoLegado() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:legacy-migration;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE corretora (id BIGINT PRIMARY KEY, cnpj VARCHAR(255))");
        jdbc.execute("CREATE TABLE produto (id BIGINT PRIMARY KEY, ticker VARCHAR(255), "
                + "corretora_id BIGINT, CONSTRAINT fk_produto_corretora "
                + "FOREIGN KEY (corretora_id) REFERENCES corretora(id))");
        jdbc.execute("CREATE TABLE operacao_acao (id BIGINT PRIMARY KEY, acao_id BIGINT, "
                + "CONSTRAINT fk_operacao_acao_produto "
                + "FOREIGN KEY (acao_id) REFERENCES produto(id))");
        return dataSource;
    }

    private long contar(JdbcTemplate jdbc, String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }
}
