package com.av2.gestaodeacao.cliente;

import com.av2.gestaodeacao.cliente.dto.ViaCepResponse;
import com.av2.gestaodeacao.cliente.support.ProvedorHttpExecutor;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ViaCepCliente {

    private final RestClient restClient;
    private final ProvedorHttpExecutor executor;

    public ViaCepCliente(
            RestClient.Builder builder,
            ProvedorHttpExecutor executor,
            @Value("${integracoes.viacep.base-url}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.executor = executor;
    }

    public ViaCepResponse buscarCep(String cep) {
        return executor.executar("ViaCEP", () -> validar(restClient.get()
                .uri("/ws/{cep}/json/", cep)
                .retrieve()
                .body(ViaCepResponse.class)));
    }

    private ViaCepResponse validar(ViaCepResponse response) {
        if (response == null || Boolean.TRUE.equals(response.erro())) {
            throw new DadoExternoNaoEncontradoException("CEP não encontrado no ViaCEP");
        }
        if (vazio(response.logradouro()) || vazio(response.bairro()) ||
                vazio(response.localidade()) || vazio(response.uf())) {
            throw new FalhaDeProvedorException("ViaCEP");
        }
        return response;
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
