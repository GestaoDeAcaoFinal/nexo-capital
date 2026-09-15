package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.exceptions.RecursoNaoEncontradoException;
import com.av2.gestaodeacao.services.AcaoService;
import com.av2.gestaodeacao.services.CorretoraService;
import com.av2.gestaodeacao.services.OperacaoAcaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "TWELVE_DATA_API_KEY=test-key")
@AutoConfigureMockMvc
class AcaoResourceMvcTest {

    @MockitoBean private AcaoService acaoService;
    @MockitoBean private CorretoraService corretoraService;
    @MockitoBean private OperacaoAcaoService operacaoService;
    @Autowired private MockMvc mockMvc;

    @Test
    void listaEBuscaAcoesPorIdETicker() throws Exception {
        Acao acao = acao(1L, "PETR4");
        when(acaoService.listar()).thenReturn(List.of(acao));
        when(acaoService.buscarPorId(1L)).thenReturn(acao);
        when(acaoService.buscarPorTicker("petr4")).thenReturn(acao);

        mockMvc.perform(get("/acoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ticker").value("PETR4"));
        mockMvc.perform(get("/acoes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get("/acoes/ticker/petr4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("PETR4"));

        verify(acaoService).buscarPorTicker("petr4");
    }

    @Test
    void buscasAusentesDeAcaoRetornamProblemDetail404() throws Exception {
        when(acaoService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Ação não encontrada"));
        when(acaoService.buscarPorTicker("ABCD"))
                .thenThrow(new RecursoNaoEncontradoException("Ação não encontrada"));

        mockMvc.perform(get("/acoes/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/acoes/ticker/ABCD"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    private Acao acao(Long id, String ticker) {
        Acao acao = new Acao();
        acao.setId(id);
        acao.setTicker(ticker);
        return acao;
    }
}
