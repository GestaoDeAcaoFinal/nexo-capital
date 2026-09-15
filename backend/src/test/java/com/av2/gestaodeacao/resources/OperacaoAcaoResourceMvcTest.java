package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.OperacaoAcao;
import com.av2.gestaodeacao.domains.enums.TipoOperacao;
import com.av2.gestaodeacao.domains.dtos.PosicaoCarteiraDTO;
import com.av2.gestaodeacao.domains.dtos.ResumoCarteiraDTO;
import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "TWELVE_DATA_API_KEY=test-key")
@AutoConfigureMockMvc
class OperacaoAcaoResourceMvcTest {

    @MockitoBean private AcaoService acaoService;
    @MockitoBean private CorretoraService corretoraService;
    @MockitoBean private OperacaoAcaoService operacaoService;
    @Autowired private MockMvc mockMvc;

    @Test
    void listaTotalHistoricoEFiltraComprasEVendas() throws Exception {
        OperacaoAcao compra = operacao(1L, TipoOperacao.COMPRA);
        OperacaoAcao venda = operacao(2L, TipoOperacao.VENDA);
        when(operacaoService.listar()).thenReturn(List.of(compra, venda));
        when(operacaoService.listarHistorico(10L)).thenReturn(List.of(compra, venda));
        when(operacaoService.listarCompras()).thenReturn(List.of(compra));
        when(operacaoService.listarVendas()).thenReturn(List.of(venda));

        mockMvc.perform(get("/operacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoOperacao").value("COMPRA"))
                .andExpect(jsonPath("$[1].tipoOperacao").value("VENDA"));
        mockMvc.perform(get("/operacoes/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
        mockMvc.perform(get("/operacoes/compras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoOperacao").value("COMPRA"));
        mockMvc.perform(get("/operacoes/vendas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipoOperacao").value("VENDA"));

        verify(operacaoService).listarHistorico(10L);
    }

    @Test
    void buscaOperacaoPorId() throws Exception {
        when(operacaoService.buscarPorId(7L)).thenReturn(operacao(7L, TipoOperacao.COMPRA));

        mockMvc.perform(get("/operacoes/buscar/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.tipoOperacao").value("COMPRA"));
    }

    @Test
    void retornaResumoDaCarteiraNoCampoDeOperacoes() throws Exception {
        PosicaoCarteiraDTO posicao = new PosicaoCarteiraDTO(
                10L, "PETR4", 3, 25.0, 75.0, 40.0, 120.0
        );
        when(operacaoService.resumirCarteira()).thenReturn(
                new ResumoCarteiraDTO(List.of(posicao), 3, 25.0, 75.0, 120.0)
        );

        mockMvc.perform(get("/operacoes/carteira"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeTotal").value(3))
                .andExpect(jsonPath("$.precoMedioCarteira").value(25.0))
                .andExpect(jsonPath("$.custoTotalCarteira").value(75.0))
                .andExpect(jsonPath("$.valorAtualCarteira").value(120.0))
                .andExpect(jsonPath("$.posicoes[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.posicoes[0].quantidadeAtual").value(3));
    }

    @Test
    void acaoOuOperacaoAusenteRetornaProblemDetail404() throws Exception {
        when(operacaoService.listarHistorico(99L))
                .thenThrow(new RecursoNaoEncontradoException("Ação não encontrada"));
        when(operacaoService.buscarPorId(98L))
                .thenThrow(new RecursoNaoEncontradoException("Operação não encontrada"));

        mockMvc.perform(get("/operacoes/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/operacoes/buscar/98"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void vendaSemPosicaoRetorna400EVendaAcimaDoSaldoRetorna409() throws Exception {
        when(operacaoService.vender(10L, 1, 12.0))
                .thenThrow(new EntradaOuOperacaoInvalidaException("Não existe posição comprada"));
        when(operacaoService.vender(10L, 6, 12.0))
                .thenThrow(new ConflitoException("Quantidade excede a posição disponível"));

        mockMvc.perform(post("/operacoes/vender/10")
                        .param("quantidade", "1")
                        .param("precoVenda", "12.0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(post("/operacoes/vender/10")
                        .param("quantidade", "6")
                        .param("precoVenda", "12.0"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void compraAceitaPrecoEditadoERepassaAoServico() throws Exception {
        OperacaoAcao compra = operacao(3L, TipoOperacao.COMPRA);
        compra.setPrecoUnitario(30.0);
        compra.setPrecoMedio(30.0);
        when(operacaoService.comprar(10L, 1, 30.0)).thenReturn(compra);

        mockMvc.perform(post("/operacoes/comprar/10")
                        .param("quantidade", "1")
                        .param("precoCompra", "30.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precoUnitario").value(30.0))
                .andExpect(jsonPath("$.precoMedio").value(30.0));

        verify(operacaoService).comprar(10L, 1, 30.0);
    }

    private OperacaoAcao operacao(Long id, TipoOperacao tipo) {
        Acao acao = new Acao();
        acao.setId(10L);
        acao.setTicker("PETR4");
        OperacaoAcao operacao = new OperacaoAcao();
        operacao.setId(id);
        operacao.setAcao(acao);
        operacao.setTipoOperacao(tipo);
        operacao.setQuantidade(1);
        operacao.setPrecoUnitario(10.0);
        return operacao;
    }
}
