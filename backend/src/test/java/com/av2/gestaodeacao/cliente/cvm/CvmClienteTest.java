package com.av2.gestaodeacao.cliente.cvm;

import com.av2.gestaodeacao.cliente.support.ProvedorHttpExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CvmClienteTest {

    @Test
    void baixaOZipOficialComoBytesSemAcessarAInternet() {
        byte[] zipFixture = {0x50, 0x4b, 0x03, 0x04};
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CvmCliente cliente = new CvmCliente(
                builder, new ProvedorHttpExecutor(), "https://cvm.test", "/cad_intermed.zip");
        server.expect(requestTo("https://cvm.test/cad_intermed.zip"))
                .andRespond(withSuccess(zipFixture, MediaType.APPLICATION_OCTET_STREAM));

        assertThat(cliente.baixarCadastro()).containsExactly(zipFixture);
        server.verify();
    }
}
