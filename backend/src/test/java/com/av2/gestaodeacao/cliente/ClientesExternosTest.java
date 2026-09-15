package com.av2.gestaodeacao.cliente;

import com.av2.gestaodeacao.cliente.dto.BrapiResponse;
import com.av2.gestaodeacao.cliente.dto.BrasilApiCnpjResponse;
import com.av2.gestaodeacao.cliente.dto.TwelveDataResponse;
import com.av2.gestaodeacao.cliente.dto.ViaCepResponse;
import com.av2.gestaodeacao.cliente.support.ProvedorHttpExecutor;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClientesExternosTest {

    private final ProvedorHttpExecutor executor = new ProvedorHttpExecutor();

    @Test
    void buscaTaxaDeCambioEntreAMoedaDaAcaoEOReal() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TwelveDataCliente cliente = new TwelveDataCliente(
                builder, executor, "https://twelve.test", "test-key");
        server.expect(once(), requestTo(
                        "https://twelve.test/exchange_rate?symbol=USD/BRL&apikey=test-key"))
                .andRespond(withSuccess("""
                        {"symbol":"USD/BRL","rate":5.25,"timestamp":1789344000}
                        """, MediaType.APPLICATION_JSON));

        assertThat(cliente.buscarTaxaCambio("USD", "BRL"))
                .isEqualByComparingTo("5.25");
        server.verify();
    }

    @Test
    void desserializaAsQuatroRespostasEmTiposConcretos() {
        RestClient.Builder brapiBuilder = RestClient.builder();
        MockRestServiceServer brapiServer = MockRestServiceServer.bindTo(brapiBuilder).build();
        AcaoCliente brapi = new AcaoCliente(brapiBuilder, executor, "https://brapi.test");
        brapiServer.expect(once(), requestTo("https://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess("""
                        {"results":[{"longName":"Petrobras","regularMarketPrice":31.42,"currency":"BRL"}]}
                        """, MediaType.APPLICATION_JSON));

        RestClient.Builder brasilApiBuilder = RestClient.builder();
        MockRestServiceServer brasilApiServer = MockRestServiceServer.bindTo(brasilApiBuilder).build();
        CnpjCliente brasilApi = new CnpjCliente(brasilApiBuilder, executor, "https://brasilapi.test");
        brasilApiServer.expect(requestTo("https://brasilapi.test/api/cnpj/v1/191"))
                .andRespond(withSuccess("""
                        {"razao_social":"Corretora SA","nome_fantasia":"Corretora","email":"a@b.com",\
                        "ddd_telefone_1":"1133334444","descricao_situacao_cadastral":"ATIVA"}
                        """, MediaType.APPLICATION_JSON));

        RestClient.Builder viaCepBuilder = RestClient.builder();
        MockRestServiceServer viaCepServer = MockRestServiceServer.bindTo(viaCepBuilder).build();
        ViaCepCliente viaCep = new ViaCepCliente(viaCepBuilder, executor, "https://viacep.test");
        viaCepServer.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withSuccess("""
                        {"logradouro":"Praça da Sé","bairro":"Sé","localidade":"São Paulo","uf":"SP"}
                        """, MediaType.APPLICATION_JSON));

        RestClient.Builder twelveBuilder = RestClient.builder();
        MockRestServiceServer twelveServer = MockRestServiceServer.bindTo(twelveBuilder).build();
        TwelveDataCliente twelve = new TwelveDataCliente(twelveBuilder, executor, "https://twelve.test", "test-key");
        twelveServer.expect(requestTo("https://twelve.test/quote?symbol=AAPL&apikey=test-key"))
                .andRespond(withSuccess("""
                        {"name":"Apple Inc","close":"223.10","currency":"USD"}
                        """, MediaType.APPLICATION_JSON));

        BrapiResponse brapiResponse = brapi.buscarAcao("PETR4");
        BrasilApiCnpjResponse cnpjResponse = brasilApi.buscarCnpj("191");
        ViaCepResponse cepResponse = viaCep.buscarCep("01001000");
        TwelveDataResponse twelveResponse = twelve.buscarAcao("AAPL");

        assertThat(brapiResponse.results().get(0).regularMarketPrice()).isEqualByComparingTo("31.42");
        assertThat(cnpjResponse.razaoSocial()).isEqualTo("Corretora SA");
        assertThat(cepResponse.localidade()).isEqualTo("São Paulo");
        assertThat(twelveResponse.close()).isEqualByComparingTo("223.10");
        brapiServer.verify();
        brasilApiServer.verify();
        viaCepServer.verify();
        twelveServer.verify();
    }

    @Test
    void rejeitaCampoObrigatorioAusenteETipoIncompativel() {
        RestClient.Builder missingBuilder = RestClient.builder();
        MockRestServiceServer missingServer = MockRestServiceServer.bindTo(missingBuilder).build();
        AcaoCliente missingClient = new AcaoCliente(missingBuilder, executor, "https://brapi.test");
        missingServer.expect(requestTo("https://brapi.test/api/quote/PETR4"))
                .andRespond(withSuccess("{\"results\":[{\"longName\":\"Petrobras\",\"currency\":\"BRL\"}]}", MediaType.APPLICATION_JSON));

        RestClient.Builder wrongTypeBuilder = RestClient.builder();
        MockRestServiceServer wrongTypeServer = MockRestServiceServer.bindTo(wrongTypeBuilder).build();
        CnpjCliente wrongTypeClient = new CnpjCliente(wrongTypeBuilder, executor, "https://brasilapi.test");
        wrongTypeServer.expect(requestTo("https://brasilapi.test/api/cnpj/v1/191"))
                .andRespond(withSuccess("{\"razao_social\":{\"valor\":\"inválido\"}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> missingClient.buscarAcao("PETR4"))
                .isInstanceOf(FalhaDeProvedorException.class);
        assertThatThrownBy(() -> wrongTypeClient.buscarCnpj("191"))
                .isInstanceOf(FalhaDeProvedorException.class);
    }

    @Test
    void traduzAusenciaStatusDeErroConteudoInvalidoETimeout() {
        RestClient.Builder notFoundBuilder = RestClient.builder();
        MockRestServiceServer notFoundServer = MockRestServiceServer.bindTo(notFoundBuilder).build();
        ViaCepCliente notFoundClient = new ViaCepCliente(notFoundBuilder, executor, "https://viacep.test");
        notFoundServer.expect(requestTo("https://viacep.test/ws/00000000/json/"))
                .andRespond(withResourceNotFound());

        RestClient.Builder statusBuilder = RestClient.builder();
        MockRestServiceServer statusServer = MockRestServiceServer.bindTo(statusBuilder).build();
        AcaoCliente statusClient = new AcaoCliente(statusBuilder, executor, "https://brapi.test");
        statusServer.expect(requestTo("https://brapi.test/api/quote/PETR4"))
                .andRespond(withServerError());

        RestClient.Builder invalidBuilder = RestClient.builder();
        MockRestServiceServer invalidServer = MockRestServiceServer.bindTo(invalidBuilder).build();
        TwelveDataCliente invalidClient = new TwelveDataCliente(invalidBuilder, executor, "https://twelve.test", "secret-key");
        invalidServer.expect(requestTo("https://twelve.test/quote?symbol=AAPL&apikey=secret-key"))
                .andRespond(withSuccess("não-é-json", MediaType.APPLICATION_JSON));

        RestClient.Builder timeoutBuilder = RestClient.builder();
        MockRestServiceServer timeoutServer = MockRestServiceServer.bindTo(timeoutBuilder).build();
        CnpjCliente timeoutClient = new CnpjCliente(timeoutBuilder, executor, "https://brasilapi.test");
        timeoutServer.expect(requestTo("https://brasilapi.test/api/cnpj/v1/191"))
                .andRespond(withException(new SocketTimeoutException("https://host?apikey=secret-key")));

        assertThatThrownBy(() -> notFoundClient.buscarCep("00000000"))
                .isInstanceOf(DadoExternoNaoEncontradoException.class);
        assertThatThrownBy(() -> statusClient.buscarAcao("PETR4"))
                .isInstanceOf(FalhaDeProvedorException.class);
        assertThatThrownBy(() -> invalidClient.buscarAcao("AAPL"))
                .isInstanceOf(FalhaDeProvedorException.class)
                .hasMessageNotContaining("secret-key");
        assertThatThrownBy(() -> timeoutClient.buscarCnpj("191"))
                .isInstanceOf(ProvedorIndisponivelException.class)
                .hasMessageNotContaining("secret-key");
    }
}
