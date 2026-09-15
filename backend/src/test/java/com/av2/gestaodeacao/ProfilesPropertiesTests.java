package com.av2.gestaodeacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

class ProfilesPropertiesTests {

    private static final String TEST_API_KEY =
            "TWELVE_DATA_API_KEY=test-key";

    @Test
    void execucaoLocalSemPerfilUsaH2ComoPadrao() {

        try (ConfigurableApplicationContext context =
                     loadDefaultProfile(TEST_API_KEY)) {

            Environment env = context.getEnvironment();

            assertThat(env.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:h2:mem:testdb");

            assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("update");
        }
    }

    @Test
    void profileH2CarregaConfiguracaoCorreta() {

        try (ConfigurableApplicationContext context =
                     loadProfile("h2", TEST_API_KEY)) {

            Environment env = context.getEnvironment();

            assertThat(env.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:h2:mem:testdb");

            assertThat(env.getProperty("spring.datasource.driver-class-name"))
                    .isEqualTo("org.h2.Driver");

            assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("update");
        }
    }

    @Test
    void profileDevCarregaConfiguracaoCorreta() {

        try (ConfigurableApplicationContext context =
                     loadProfile(
                             "dev",
                             "DB_URL=jdbc:postgresql://localhost:5432/gestaodeacao",
                             "DB_USERNAME=postgres",
                             "DB_PASSWORD=teste",
                             TEST_API_KEY
                     )) {

            Environment env = context.getEnvironment();

            assertThat(env.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:postgresql://localhost:5432/gestaodeacao");

            assertThat(env.getProperty("spring.datasource.driver-class-name"))
                    .isEqualTo("org.postgresql.Driver");
        }
    }

    @Test
    void profileMysqlCarregaConfiguracaoCorreta() {

        try (ConfigurableApplicationContext context =
                     loadProfile(
                             "mysql",
                             "DB_URL=jdbc:mysql://localhost:3306/gestaodeacao",
                             "DB_USERNAME=root",
                             "DB_PASSWORD=teste",
                             TEST_API_KEY
                     )) {

            Environment env = context.getEnvironment();

            assertThat(env.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:mysql://localhost:3306/gestaodeacao");

            assertThat(env.getProperty("spring.datasource.driver-class-name"))
                    .isEqualTo("com.mysql.cj.jdbc.Driver");

            assertThat(env.getProperty(
                    "spring.jpa.properties.hibernate.type.preferred_boolean_jdbc_type"))
                    .isEqualTo("TINYINT");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "mysql"})
    void perfilExternoAceitaSobrescritaExplicitaDoDdlAuto(String profile) {

        try (ConfigurableApplicationContext context =
                     loadProfile(
                             profile,
                             "DB_URL=jdbc:test://localhost/gestaodeacao",
                             "DB_USERNAME=test-user",
                             "DB_PASSWORD=test-password",
                             "DDL_AUTO=none",
                             TEST_API_KEY
                     )) {

            assertThat(context.getEnvironment()
                    .getProperty("spring.jpa.hibernate.ddl-auto"))
                    .isEqualTo("none");
        }
    }

    private ConfigurableApplicationContext loadProfile(String profile, String... properties) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        return loadContext(context, properties);
    }

    private ConfigurableApplicationContext loadDefaultProfile(String... properties) {
        return loadContext(new AnnotationConfigApplicationContext(), properties);
    }

    private ConfigurableApplicationContext loadContext(
            AnnotationConfigApplicationContext context,
            String... properties
    ) {
        TestPropertyValues.of(properties).applyTo(context);
        new ConfigDataApplicationContextInitializer().initialize(context);
        context.refresh();
        return context;
    }
}
