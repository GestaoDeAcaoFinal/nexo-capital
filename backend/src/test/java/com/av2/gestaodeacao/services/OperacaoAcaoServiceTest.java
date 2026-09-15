package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.cliente.TwelveDataCliente;
import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.OperacaoAcao;
import com.av2.gestaodeacao.domains.dtos.ResumoCarteiraDTO;
import com.av2.gestaodeacao.domains.enums.TipoOperacao;
import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;
import com.av2.gestaodeacao.exceptions.RecursoNaoEncontradoException;
import com.av2.gestaodeacao.repositories.AcaoRepository;
import com.av2.gestaodeacao.repositories.OperacaoAcaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OperacaoAcaoServiceTest {

    private AcaoRepository acaoRepository;
    private OperacaoAcaoRepository operacaoRepository;
    private TwelveDataCliente twelveDataCliente;
    private OperacaoAcaoService service;

    @BeforeEach
    void configurar() {
        acaoRepository = mock(AcaoRepository.class);
        operacaoRepository = mock(OperacaoAcaoRepository.class);
        twelveDataCliente = mock(TwelveDataCliente.class);
        service = new OperacaoAcaoService();
        ReflectionTestUtils.setField(service, "repository", acaoRepository);
        ReflectionTestUtils.setField(service, "operacaoRepository", operacaoRepository);
        ReflectionTestUtils.setField(service, "twelveDataCliente", twelveDataCliente);
        when(operacaoRepository.save(any(OperacaoAcao.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void primeiraCompraRegistraCamposObrigatoriosEEstabelecePrecoMedio() {
        Acao acao = acao(1L, 25.50);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA))
                .thenReturn(List.of());

        OperacaoAcao resultado = service.comprar(1L, 4);

        assertThat(resultado.getAcao()).isSameAs(acao);
        assertThat(resultado.getTipoOperacao()).isEqualTo(TipoOperacao.COMPRA);
        assertThat(resultado.getQuantidade()).isEqualTo(4);
        assertThat(resultado.getPrecoUnitario()).isEqualTo(25.50);
        assertThat(resultado.getValorTotal()).isEqualTo(102.0);
        assertThat(resultado.getPrecoMedio()).isEqualTo(25.50);
        assertThat(resultado.getDataOperacao()).isNotNull();
        verify(operacaoRepository).save(resultado);
    }

    @Test
    void primeiraCompraUsaPrecoInformadoMesmoQuandoDiferenteDaCotacaoAtual() {
        Acao acao = acao(1L, 35.0);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA))
                .thenReturn(List.of());

        OperacaoAcao resultado = service.comprar(1L, 1, 30.0);

        assertThat(resultado.getPrecoUnitario()).isEqualTo(30.0);
        assertThat(resultado.getPrecoMedio()).isEqualTo(30.0);
        assertThat(resultado.getValorTotal()).isEqualTo(30.0);
    }

    @Test
    void comprasDaMesmaAcaoCalculamPrecoMedioPonderadoPeloPrecoInformado() {
        Acao aapl = acao(1L, 22.0);
        aapl.setTicker("AAPL");
        OperacaoAcao compraA30 = operacao(aapl, TipoOperacao.COMPRA, 1, 30.0, 30.0);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(aapl));
        when(operacaoRepository.findByAcaoAndTipoOperacao(aapl, TipoOperacao.COMPRA))
                .thenReturn(List.of(compraA30));
        when(operacaoRepository.findByAcaoAndTipoOperacao(aapl, TipoOperacao.VENDA))
                .thenReturn(List.of());

        OperacaoAcao resultado = service.comprar(1L, 1, 20.0);

        assertThat(resultado.getPrecoMedio()).isEqualTo(25.0);
    }

    @Test
    void novaCompraCalculaMediaPonderadaDaPosicaoAtual() {
        Acao acao = acao(1L, 130.0);
        OperacaoAcao compraAnterior = operacao(acao, TipoOperacao.COMPRA, 10, 100.0, 100.0);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA))
                .thenReturn(List.of(compraAnterior));

        OperacaoAcao resultado = service.comprar(1L, 20);

        assertThat(resultado.getPrecoMedio()).isEqualTo(120.0);
        verify(operacaoRepository).findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA);
        verify(operacaoRepository).findByAcaoAndTipoOperacao(acao, TipoOperacao.VENDA);
    }

    @Test
    void novaCompraDepoisDeVendaUsaSomenteOCustoDaPosicaoRestante() {
        Acao acao = acao(1L, 40.0);
        OperacaoAcao compraA20 = operacao(acao, TipoOperacao.COMPRA, 1, 20.0, 20.0);
        OperacaoAcao compraA30 = operacao(acao, TipoOperacao.COMPRA, 1, 30.0, 25.0);
        OperacaoAcao venda = operacao(acao, TipoOperacao.VENDA, 1, 35.0, 25.0);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA))
                .thenReturn(List.of(compraA20, compraA30));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.VENDA))
                .thenReturn(List.of(venda));

        OperacaoAcao resultado = service.comprar(1L, 1);

        assertThat(resultado.getPrecoMedio()).isEqualTo(32.5);
    }

    @Test
    void compraInvalidaOuSemCotacaoNaoPersiste() {
        assertThatThrownBy(() -> service.comprar(1L, 0))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
        verifyNoInteractions(acaoRepository, operacaoRepository);

        Acao semCotacao = acao(1L, 0.0);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(semCotacao));
        assertThatThrownBy(() -> service.comprar(1L, 1))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
        verify(operacaoRepository, never()).save(any());

        semCotacao.setCotacaoAtual(Double.NaN);
        assertThatThrownBy(() -> service.comprar(1L, 1))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
    }

    @Test
    void compraOuVendaDeAcaoInexistenteNaoPersiste() {
        when(acaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.comprar(99L, 1))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.vender(99L, 1, 10.0))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(operacaoRepository, never()).save(any());
    }

    @Test
    void vendaValidaUsaMediaPonderadaMesmoSeRepositorioNaoOrdenarCompras() {
        Acao acao = acao(1L, 20.0);
        OperacaoAcao primeira = operacao(acao, TipoOperacao.COMPRA, 2, 10.0, 10.0);
        OperacaoAcao segunda = operacao(acao, TipoOperacao.COMPRA, 2, 20.0, 15.0);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA))
                .thenReturn(List.of(segunda, primeira));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.VENDA))
                .thenReturn(List.of());

        OperacaoAcao resultado = service.vender(1L, 1, 20.0);

        assertThat(resultado.getTipoOperacao()).isEqualTo(TipoOperacao.VENDA);
        assertThat(resultado.getPrecoMedio()).isEqualTo(15.0);
        assertThat(resultado.getLucroPrejuizo()).isEqualTo(5.0);
        assertThat(resultado.getValorTotal()).isEqualTo(20.0);
        assertThat(resultado.getDataOperacao()).isNotNull();
    }

    @Test
    void vendaSemCompraRetornaRegraInvalidaSemPersistir() {
        Acao acao = acao(1L, 10.0);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.vender(1L, 1, 12.0))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
        verify(operacaoRepository, never()).save(any());
    }

    @Test
    void vendaAcimaDoSaldoRetornaConflitoSemPersistir() {
        Acao acao = acao(1L, 10.0);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA))
                .thenReturn(List.of(operacao(acao, TipoOperacao.COMPRA, 5, 10.0, 10.0)));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.VENDA))
                .thenReturn(List.of(operacao(acao, TipoOperacao.VENDA, 3, 12.0, 10.0)));

        assertThatThrownBy(() -> service.vender(1L, 3, 12.0))
                .isInstanceOf(ConflitoException.class);
        verify(operacaoRepository, never()).save(any());
    }

    @Test
    void vendaValidaConsideraSaldoCompradoMenosVendido() {
        Acao acao = acao(1L, 10.0);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA))
                .thenReturn(List.of(operacao(acao, TipoOperacao.COMPRA, 5, 10.0, 10.0)));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.VENDA))
                .thenReturn(List.of(operacao(acao, TipoOperacao.VENDA, 3, 12.0, 10.0)));

        OperacaoAcao resultado = service.vender(1L, 2, 11.0);

        assertThat(resultado.getQuantidade()).isEqualTo(2);
        verify(operacaoRepository).save(resultado);
    }

    @Test
    void vendaDepoisDeVendaERecompraMantemOPrecoMedioDaPosicaoAtual() {
        Acao acao = acao(1L, 40.0);
        OperacaoAcao compraA20 = operacao(acao, TipoOperacao.COMPRA, 1, 20.0, 20.0);
        OperacaoAcao compraA30 = operacao(acao, TipoOperacao.COMPRA, 1, 30.0, 25.0);
        OperacaoAcao vendaAnterior = operacao(acao, TipoOperacao.VENDA, 1, 35.0, 25.0);
        OperacaoAcao recompra = operacao(acao, TipoOperacao.COMPRA, 1, 40.0, 32.5);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA))
                .thenReturn(List.of(compraA20, compraA30, recompra));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.VENDA))
                .thenReturn(List.of(vendaAnterior));

        OperacaoAcao resultado = service.vender(1L, 1, 45.0);

        assertThat(resultado.getPrecoMedio()).isEqualTo(32.5);
        assertThat(resultado.getLucroPrejuizo()).isEqualTo(12.5);
    }

    @Test
    void vendaRejeitaQuantidadeEPrecoNaoPositivosAntesDePersistir() {
        assertThatThrownBy(() -> service.vender(1L, 0, 10.0))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
        assertThatThrownBy(() -> service.vender(1L, 1, 0.0))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
        assertThatThrownBy(() -> service.vender(1L, 1, Double.POSITIVE_INFINITY))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);
        verifyNoInteractions(acaoRepository, operacaoRepository);
    }

    @Test
    void lucroPrejuizoEhArredondadoParaGanhoPerdaEEmpate() {
        assertThat(venderComPrecoMedio(10.0, 12.345, 2).getLucroPrejuizo()).isEqualTo(4.69);
        assertThat(venderComPrecoMedio(10.0, 8.444, 2).getLucroPrejuizo()).isEqualTo(-3.11);
        assertThat(venderComPrecoMedio(10.0, 10.0, 2).getLucroPrejuizo()).isZero();
    }

    @Test
    void consultasDelegamAosFiltrosCorretosETratamAusencias() {
        Acao acao = acao(1L, 10.0);
        OperacaoAcao compra = operacao(acao, TipoOperacao.COMPRA, 1, 10.0, 10.0);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(acaoRepository.findById(99L)).thenReturn(Optional.empty());
        when(operacaoRepository.findAll()).thenReturn(List.of(compra));
        when(operacaoRepository.findByAcao(acao)).thenReturn(List.of(compra));
        when(operacaoRepository.findByTipoOperacao(TipoOperacao.COMPRA)).thenReturn(List.of(compra));
        when(operacaoRepository.findByTipoOperacao(TipoOperacao.VENDA)).thenReturn(List.of());
        when(operacaoRepository.findById(7L)).thenReturn(Optional.of(compra));
        when(operacaoRepository.findById(8L)).thenReturn(Optional.empty());

        assertThat(service.listar()).containsExactly(compra);
        assertThat(service.listarHistorico(1L)).containsExactly(compra);
        assertThat(service.listarCompras()).containsExactly(compra);
        assertThat(service.listarVendas()).isEmpty();
        assertThat(service.buscarPorId(7L)).isSameAs(compra);
        assertThatThrownBy(() -> service.listarHistorico(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.buscarPorId(8L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void resumePosicoesAtuaisQuantidadeTotalPrecoMedioECustosDaCarteira() {
        Acao petr4 = acao(1L, 40.0);
        Acao vale3 = acao(2L, 60.0);
        vale3.setTicker("VALE3");
        OperacaoAcao petrCompraA20 = operacao(petr4, TipoOperacao.COMPRA, 2, 20.0, 20.0);
        OperacaoAcao petrCompraA30 = operacao(petr4, TipoOperacao.COMPRA, 2, 30.0, 25.0);
        OperacaoAcao petrVenda = operacao(petr4, TipoOperacao.VENDA, 1, 35.0, 25.0);
        OperacaoAcao valeCompra = operacao(vale3, TipoOperacao.COMPRA, 2, 50.0, 50.0);
        when(operacaoRepository.findAll()).thenReturn(
                List.of(petrCompraA20, petrCompraA30, petrVenda, valeCompra)
        );

        ResumoCarteiraDTO resultado = service.resumirCarteira();

        assertThat(resultado.quantidadeTotal()).isEqualTo(5);
        assertThat(resultado.precoMedioCarteira()).isEqualTo(35.0);
        assertThat(resultado.custoTotalCarteira()).isEqualTo(175.0);
        assertThat(resultado.valorAtualCarteira()).isEqualTo(240.0);
        assertThat(resultado.posicoes()).hasSize(2);
        assertThat(resultado.posicoes().get(0).ticker()).isEqualTo("PETR4");
        assertThat(resultado.posicoes().get(0).quantidadeAtual()).isEqualTo(3);
        assertThat(resultado.posicoes().get(0).precoMedio()).isEqualTo(25.0);
        assertThat(resultado.posicoes().get(0).custoTotal()).isEqualTo(75.0);
        assertThat(resultado.posicoes().get(0).valorAtual()).isEqualTo(120.0);
    }

    @Test
    void converteValoresDeAcaoEmDolarParaReaisNoResumoDaCarteira() {
        Acao aapl = acao(3L, 120.0);
        aapl.setTicker("AAPL");
        aapl.setMoeda("USD");
        OperacaoAcao compra = operacao(aapl, TipoOperacao.COMPRA, 2, 100.0, 100.0);
        when(operacaoRepository.findAll()).thenReturn(List.of(compra));
        when(twelveDataCliente.buscarTaxaCambio("USD", "BRL"))
                .thenReturn(new BigDecimal("5.25"));

        ResumoCarteiraDTO resultado = service.resumirCarteira();

        assertThat(resultado.precoMedioCarteira()).isEqualTo(525.0);
        assertThat(resultado.custoTotalCarteira()).isEqualTo(1050.0);
        assertThat(resultado.valorAtualCarteira()).isEqualTo(1260.0);
        assertThat(resultado.posicoes().get(0).precoMedio()).isEqualTo(525.0);
        assertThat(resultado.posicoes().get(0).custoTotal()).isEqualTo(1050.0);
        assertThat(resultado.posicoes().get(0).cotacaoAtual()).isEqualTo(630.0);
        assertThat(resultado.posicoes().get(0).valorAtual()).isEqualTo(1260.0);
    }

    @Test
    void carteiraVaziaDepoisDaVendaTotalRetornaTotaisZerados() {
        Acao acao = acao(1L, 40.0);
        OperacaoAcao compra = operacao(acao, TipoOperacao.COMPRA, 2, 25.0, 25.0);
        OperacaoAcao venda = operacao(acao, TipoOperacao.VENDA, 2, 30.0, 25.0);
        when(operacaoRepository.findAll()).thenReturn(List.of(compra, venda));

        ResumoCarteiraDTO resultado = service.resumirCarteira();

        assertThat(resultado.posicoes()).isEmpty();
        assertThat(resultado.quantidadeTotal()).isZero();
        assertThat(resultado.precoMedioCarteira()).isZero();
        assertThat(resultado.custoTotalCarteira()).isZero();
        assertThat(resultado.valorAtualCarteira()).isZero();
    }

    private OperacaoAcao venderComPrecoMedio(double precoMedio, double precoVenda, int quantidade) {
        Acao acao = acao(1L, precoMedio);
        OperacaoAcao compra = operacao(acao, TipoOperacao.COMPRA, 10, precoMedio, precoMedio);
        when(acaoRepository.findById(1L)).thenReturn(Optional.of(acao));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA))
                .thenReturn(List.of(compra));
        when(operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.VENDA))
                .thenReturn(List.of());
        return service.vender(1L, quantidade, precoVenda);
    }

    private Acao acao(Long id, Double cotacao) {
        Acao acao = new Acao();
        acao.setId(id);
        acao.setTicker("PETR4");
        acao.setCotacaoAtual(cotacao);
        return acao;
    }

    private OperacaoAcao operacao(Acao acao, TipoOperacao tipo, int quantidade,
                                   double precoUnitario, double precoMedio) {
        OperacaoAcao operacao = new OperacaoAcao();
        operacao.setAcao(acao);
        operacao.setTipoOperacao(tipo);
        operacao.setQuantidade(quantidade);
        operacao.setPrecoUnitario(precoUnitario);
        operacao.setPrecoMedio(precoMedio);
        return operacao;
    }
}
