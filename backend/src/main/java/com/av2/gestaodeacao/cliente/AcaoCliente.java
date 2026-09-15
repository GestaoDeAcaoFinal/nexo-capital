package com.av2.gestaodeacao.cliente;

import com.av2.gestaodeacao.cliente.dto.BrapiQuoteResponse;
import com.av2.gestaodeacao.cliente.dto.BrapiResponse;
import com.av2.gestaodeacao.cliente.support.ProvedorHttpExecutor;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AcaoCliente {

    private final RestClient restClient;
    private final ProvedorHttpExecutor executor;

    public AcaoCliente(
            RestClient.Builder builder,
            ProvedorHttpExecutor executor,
            @Value("${integracoes.brapi.base-url}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.executor = executor;
    }

    public BrapiResponse buscarAcao(String ticker) {
        return executor.executar("Brapi", () -> validar(restClient.get()
                .uri("/api/quote/{ticker}", ticker)
                .retrieve()
                .body(BrapiResponse.class)));
    }

    private BrapiResponse validar(BrapiResponse response) {
        if (response == null || response.results() == null || response.results().isEmpty()) {
            throw new DadoExternoNaoEncontradoException("Ticker não encontrado na Brapi");
        }
        BrapiQuoteResponse quote = response.results().get(0);
        if (quote == null || vazio(quote.longName()) || quote.regularMarketPrice() == null ||
                quote.regularMarketPrice().signum() <= 0 ||
                vazio(quote.currency())) {
            throw new FalhaDeProvedorException("Brapi");
        }
        return response;
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
