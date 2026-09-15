package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import com.av2.gestaodeacao.exceptions.RecursoNaoEncontradoException;
import com.av2.gestaodeacao.services.AcaoService;
import com.av2.gestaodeacao.services.CorretoraService;
import com.av2.gestaodeacao.services.OperacaoAcaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "TWELVE_DATA_API_KEY=test-key")
@AutoConfigureMockMvc
class ApiContractMvcTest {

    @MockitoBean
    private AcaoService acaoService;

    @MockitoBean
    private CorretoraService corretoraService;

    @MockitoBean
    private OperacaoAcaoService operacaoService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void payloadDeAcaoInvalidoRetornaProblemDetailSemInvocarServico() throws Exception {
        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\" \",\"corretoraId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.errors", hasKey("ticker")))
                .andExpect(jsonPath("$.errors", hasKey("corretoraId")));

        verifyNoInteractions(acaoService);
    }

    @Test
    void payloadDeCorretoraInvalidoRetornaErrosDosCamposSemInvocarServico() throws Exception {
        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"123\",\"cep\":\"12A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors", hasKey("cnpj")))
                .andExpect(jsonPath("$.errors", hasKey("cep")));

        verifyNoInteractions(corretoraService);
    }

    @Test
    void parametrosNaoPositivosSaoRejeitadosAntesDoServico() throws Exception {
        mockMvc.perform(put("/acoes/0/atualizar-cotacao"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/operacoes/comprar/1").param("quantidade", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/operacoes/vender/1")
                        .param("quantidade", "1")
                        .param("precoVenda", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(acaoService, operacaoService);
    }

    @Test
    void requisicoesMalformadasMantemProblemDetailEstavel() throws Exception {
        mockMvc.perform(post("/acoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requisição inválida"));

        mockMvc.perform(post("/operacoes/comprar/1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requisição inválida"));

        verifyNoInteractions(acaoService, operacaoService);
    }

    @ParameterizedTest
    @MethodSource("errosDeDominio")
    void excecoesConhecidasProduzemMatrizProblemDetail(
            RuntimeException exception,
            int statusEsperado,
            String tituloEsperado
    ) throws Exception {
        reset(acaoService);
        when(acaoService.buscarPorId(anyLong())).thenThrow(exception);

        String corpo = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/acoes/1"))
                .andExpect(status().is(statusEsperado))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(statusEsperado))
                .andExpect(jsonPath("$.title").value(tituloEsperado))
                .andExpect(jsonPath("$.detail").isString())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(corpo)
                .doesNotContain("chave-super-secreta")
                .doesNotContain("senha=segredo")
                .doesNotContain("https://provedor.exemplo/quote?apikey=")
                .doesNotContain("payload-externo-confidencial");
    }

    private static Stream<Arguments> errosDeDominio() {
        RuntimeException causaSensivel = new RuntimeException(
                "chave-super-secreta senha=segredo "
                        + "https://provedor.exemplo/quote?apikey=chave-super-secreta "
                        + "payload-externo-confidencial");
        return Stream.of(
                Arguments.of(new EntradaOuOperacaoInvalidaException("Quantidade deve ser positiva"),
                        400, "Requisição inválida"),
                Arguments.of(new RecursoNaoEncontradoException("Ação não encontrada"),
                        404, "Recurso não encontrado"),
                Arguments.of(new ConflitoException("Ticker já cadastrado"),
                        409, "Conflito"),
                Arguments.of(new DadoExternoNaoEncontradoException("Ticker não encontrado"),
                        422, "Dado externo não encontrado"),
                Arguments.of(new FalhaDeProvedorException("Twelve Data", causaSensivel),
                        502, "Falha no provedor"),
                Arguments.of(new ProvedorIndisponivelException("Twelve Data", causaSensivel),
                        503, "Provedor indisponível")
        );
    }
}
