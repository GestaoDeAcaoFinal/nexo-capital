package com.av2.gestaodeacao.cliente.support;

import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

@Component
public class ProvedorHttpExecutor {

    public <T> T executar(String provedor, Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (DadoExternoNaoEncontradoException | FalhaDeProvedorException |
                 ProvedorIndisponivelException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new ProvedorIndisponivelException(provedor, e);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new DadoExternoNaoEncontradoException(
                        "Dado não encontrado no provedor " + provedor);
            }
            throw new FalhaDeProvedorException(provedor, e);
        } catch (RuntimeException e) {
            throw new FalhaDeProvedorException(provedor, e);
        }
    }
}
