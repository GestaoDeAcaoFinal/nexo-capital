package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.exceptions.ConflitoException;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "TWELVE_DATA_API_KEY=test-key")
@AutoConfigureMockMvc
class CorretoraResourceMvcTest {

    @MockitoBean private AcaoService acaoService;
    @MockitoBean private CorretoraService corretoraService;
    @MockitoBean private OperacaoAcaoService operacaoService;
    @Autowired private MockMvc mockMvc;

    @Test
    void listaEBuscaCorretorasPorIdECnpj() throws Exception {
        Corretora corretora = corretora(1L, "12345678000190");
        when(corretoraService.listar()).thenReturn(List.of(corretora));
        when(corretoraService.buscarPorId(1L)).thenReturn(corretora);
        when(corretoraService.buscarPorCnpj("12345678000190")).thenReturn(corretora);

        mockMvc.perform(get("/corretoras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].cnpj").value("12345678000190"));
        mockMvc.perform(get("/corretoras/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get("/corretoras/cnpj/12345678000190"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("12345678000190"));

        verify(corretoraService).buscarPorCnpj("12345678000190");
    }

    @Test
    void ausenciaEmBuscaDeCorretoraRetornaProblemDetail404() throws Exception {
        when(corretoraService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Corretora não encontrada"));

        mockMvc.perform(get("/corretoras/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void duplicidadeNoCadastroDeCorretoraRetornaProblemDetail409() throws Exception {
        when(corretoraService.salvar(any()))
                .thenThrow(new ConflitoException("CNPJ já cadastrado"));

        mockMvc.perform(post("/corretoras")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"12.345.678/0001-90\",\"cep\":\"01001-000\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    private Corretora corretora(Long id, String cnpj) {
        Corretora corretora = new Corretora();
        corretora.setId(id);
        corretora.setCnpj(cnpj);
        return corretora;
    }
}
