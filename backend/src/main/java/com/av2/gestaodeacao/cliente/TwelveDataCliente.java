package com.av2.gestaodeacao.cliente;

import com.av2.gestaodeacao.cliente.dto.TwelveDataExchangeRateResponse;
import com.av2.gestaodeacao.cliente.dto.TwelveDataResponse;
import com.av2.gestaodeacao.cliente.support.ProvedorHttpExecutor;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class TwelveDataCliente {

    private final RestClient restClient;
    private final ProvedorHttpExecutor executor;
    private final String apiKey;

    public TwelveDataCliente(
            RestClient.Builder builder,
            ProvedorHttpExecutor executor,
            @Value("${integracoes.twelvedata.base-url}") String baseUrl,
            @Value("${twelvedata.api.key}") String apiKey
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.executor = executor;
        this.apiKey = apiKey;
    }

    public TwelveDataResponse buscarAcao(String ticker) {
        return executor.executar("Twelve Data", () -> validar(restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", ticker)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(TwelveDataResponse.class)));
    }

    public BigDecimal buscarTaxaCambio(String moedaOrigem, String moedaDestino) {
        String par = moedaOrigem + "/" + moedaDestino;
        return executor.executar("Twelve Data", () -> validarTaxa(restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/exchange_rate")
                        .queryParam("symbol", par)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .body(TwelveDataExchangeRateResponse.class)));
    }

    private TwelveDataResponse validar(TwelveDataResponse response) {
        if (response == null || response.code() != null || "error".equalsIgnoreCase(response.status())) {
            throw new DadoExternoNaoEncontradoException("Ticker não encontrado na Twelve Data");
        }
        if (vazio(response.name()) || response.close() == null || response.close().signum() <= 0 ||
                vazio(response.currency())) {
            throw new FalhaDeProvedorException("Twelve Data");
        }
        return response;
    }

    private BigDecimal validarTaxa(TwelveDataExchangeRateResponse response) {
        if (response == null || response.code() != null || "error".equalsIgnoreCase(response.status()) ||
                response.rate() == null || response.rate().signum() <= 0) {
            throw new FalhaDeProvedorException("Twelve Data");
        }
        return response.rate();
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
