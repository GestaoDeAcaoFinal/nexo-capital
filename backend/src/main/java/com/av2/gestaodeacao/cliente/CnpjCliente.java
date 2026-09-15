package com.av2.gestaodeacao.cliente;

import com.av2.gestaodeacao.cliente.dto.BrasilApiCnpjResponse;
import com.av2.gestaodeacao.cliente.support.ProvedorHttpExecutor;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CnpjCliente {

    private final RestClient restClient;
    private final ProvedorHttpExecutor executor;

    public CnpjCliente(
            RestClient.Builder builder,
            ProvedorHttpExecutor executor,
            @Value("${integracoes.brasilapi.base-url}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.executor = executor;
    }

    public BrasilApiCnpjResponse buscarCnpj(String cnpj) {
        return executor.executar("BrasilAPI", () -> validar(restClient.get()
                .uri("/api/cnpj/v1/{cnpj}", cnpj)
                .retrieve()
                .body(BrasilApiCnpjResponse.class)));
    }

    private BrasilApiCnpjResponse validar(BrasilApiCnpjResponse response) {
        if (response == null || response.razaoSocial() == null || response.razaoSocial().isBlank()) {
            throw new FalhaDeProvedorException("BrasilAPI");
        }
        return response;
    }
}
