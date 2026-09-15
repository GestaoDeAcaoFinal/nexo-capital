package com.av2.gestaodeacao.cliente.cvm;

import com.av2.gestaodeacao.cliente.support.ProvedorHttpExecutor;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CvmCliente implements CadastroCvmGateway {

    private final RestClient restClient;
    private final ProvedorHttpExecutor executor;
    private final String cadastroPath;

    public CvmCliente(
            RestClient.Builder builder,
            ProvedorHttpExecutor executor,
            @Value("${integracoes.cvm.base-url}") String baseUrl,
            @Value("${integracoes.cvm.cadastro-path}") String cadastroPath
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.executor = executor;
        this.cadastroPath = cadastroPath;
    }

    @Override
    public byte[] baixarCadastro() {
        return executor.executar("CVM", () -> {
            byte[] conteudo = restClient.get()
                    .uri(cadastroPath)
                    .retrieve()
                    .body(byte[].class);
            if (conteudo == null || conteudo.length == 0) {
                throw new FalhaDeProvedorException("CVM");
            }
            return conteudo;
        });
    }
}
