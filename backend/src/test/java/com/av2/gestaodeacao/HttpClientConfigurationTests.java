package com.av2.gestaodeacao;

import com.av2.gestaodeacao.cliente.AcaoCliente;
import com.av2.gestaodeacao.cliente.CnpjCliente;
import com.av2.gestaodeacao.cliente.TwelveDataCliente;
import com.av2.gestaodeacao.cliente.ViaCepCliente;
import com.av2.gestaodeacao.cliente.cvm.CvmCliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = GestaoDeAcaoApplication.class,
        properties = {
                "TWELVE_DATA_API_KEY=test-key",
                "HTTP_CLIENT_CONNECT_TIMEOUT=123ms",
                "HTTP_CLIENT_READ_TIMEOUT=456ms",
                "spring.http.clients.imperative.factory=simple"
        }
)
class HttpClientConfigurationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private AcaoCliente acaoCliente;

    @Autowired
    private CnpjCliente cnpjCliente;

    @Autowired
    private TwelveDataCliente twelveDataCliente;

    @Autowired
    private ViaCepCliente viaCepCliente;

    @Autowired
    private CvmCliente cvmCliente;

    @Test
    void aplicacaoConfiguraOsMesmosTimeoutsEmTodosOsGateways() {
        assertThat(environment.getProperty("spring.http.clients.connect-timeout"))
                .isEqualTo("123ms");
        assertThat(environment.getProperty("spring.http.clients.read-timeout"))
                .isEqualTo("456ms");

        List.of(acaoCliente, cnpjCliente, twelveDataCliente, viaCepCliente, cvmCliente)
                .forEach(this::assertTimeouts);
    }

    private void assertTimeouts(Object gateway) {
        Object restClient = ReflectionTestUtils.getField(gateway, "restClient");
        assertThat(restClient).isInstanceOf(RestClient.class);

        Object requestFactory = ReflectionTestUtils.getField(restClient, "clientRequestFactory");
        assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);
        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout"))
                .isEqualTo(123);
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout"))
                .isEqualTo(456);
    }
}
