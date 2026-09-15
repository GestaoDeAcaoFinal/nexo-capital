package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.cliente.AcaoCliente;
import com.av2.gestaodeacao.cliente.TwelveDataCliente;
import com.av2.gestaodeacao.cliente.dto.BrapiQuoteResponse;
import com.av2.gestaodeacao.cliente.dto.BrapiResponse;
import com.av2.gestaodeacao.cliente.dto.TwelveDataResponse;
import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.dtos.AcaoRequestDTO;
import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import com.av2.gestaodeacao.exceptions.RecursoNaoEncontradoException;
import com.av2.gestaodeacao.repositories.AcaoRepository;
import com.av2.gestaodeacao.repositories.CorretoraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AcaoServiceTest {

    private AcaoRepository repository;
    private CorretoraRepository corretoraRepository;
    private AcaoCliente brapi;
    private TwelveDataCliente twelveData;
    private AcaoService service;

    @BeforeEach
    void configurar() {
        repository = mock(AcaoRepository.class);
        corretoraRepository = mock(CorretoraRepository.class);
        brapi = mock(AcaoCliente.class);
        twelveData = mock(TwelveDataCliente.class);
        service = new AcaoService();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "corretoraRepository", corretoraRepository);
        ReflectionTestUtils.setField(service, "cliente", brapi);
        ReflectionTestUtils.setField(service, "twelveDataCliente", twelveData);
        ReflectionTestUtils.setField(service, "mercadoTickerClassifier", new MercadoTickerClassifier());
    }

    @Test
    void cadastraTickerBrasileiroNormalizadoUsandoSomenteBrapi() {
        prepararCadastro("PETR4");
        when(brapi.buscarAcao("PETR4")).thenReturn(brapi("Petrobras", "31.42", "BRL"));

        Acao salva = service.salvar(novaAcao(" petr4 "));

        assertThat(salva.getTicker()).isEqualTo("PETR4");
        assertThat(salva.getMercado()).isEqualTo("BR");
        assertThat(salva.getNomeEmpresa()).isEqualTo("Petrobras");
        assertThat(salva.getCotacaoAtual()).isEqualTo(31.42);
        assertThat(salva.getMoeda()).isEqualTo("BRL");
        assertThat(salva.getDataHoraCotacao()).isNotNull();
        verify(brapi).buscarAcao("PETR4");
        verifyNoInteractions(twelveData);
    }

    @Test
    void cadastraTickerAmericanoUsandoSomenteTwelveData() {
        prepararCadastro("AAPL");
        when(twelveData.buscarAcao("AAPL"))
                .thenReturn(twelve("Apple Inc", "223.10", "USD"));

        Acao salva = service.salvar(novaAcao("aapl"));

        assertThat(salva.getMercado()).isEqualTo("EUA");
        assertThat(salva.getNomeEmpresa()).isEqualTo("Apple Inc");
        assertThat(salva.getCotacaoAtual()).isEqualTo(223.10);
        verify(twelveData).buscarAcao("AAPL");
        verifyNoInteractions(brapi);
    }

    @Test
    void violacaoConcorrenteDeTickerEhTraduzidaParaConflito() {
        prepararCadastro("PETR4");
        when(brapi.buscarAcao("PETR4")).thenReturn(brapi("Petrobras", "31.42", "BRL"));
        doThrow(new DataIntegrityViolationException("uk_acoes_ticker"))
                .when(repository).flush();

        assertThatThrownBy(() -> service.salvar(novaAcao("PETR4")))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Ticker já cadastrado");
    }

    @Test
    void corretoraAusenteImpedeConsultaExternaEPersistencia() {
        when(repository.findByTicker("PETR4")).thenReturn(Optional.empty());
        when(corretoraRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salvar(novaAcao("PETR4")))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(brapi, twelveData);
        verify(repository, never()).save(any());
    }

    @Test
    void tickerInexistenteOuDadosIncompletosNaoSaoPersistidos() {
        prepararCadastro("AAPL");
        when(twelveData.buscarAcao("AAPL"))
                .thenThrow(new DadoExternoNaoEncontradoException("Ticker não encontrado"));

        assertThatThrownBy(() -> service.salvar(novaAcao("AAPL")))
                .isInstanceOf(DadoExternoNaoEncontradoException.class);
        verify(repository, never()).save(any());

        prepararCadastro("PETR4");
        when(brapi.buscarAcao("PETR4")).thenReturn(brapi(" ", "10", "BRL"));
        assertThatThrownBy(() -> service.salvar(novaAcao("PETR4")))
                .isInstanceOf(FalhaDeProvedorException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void cotacaoZeroOuNegativaNaoEhPersistida() {
        prepararCadastro("PETR4");
        when(brapi.buscarAcao("PETR4")).thenReturn(brapi("Petrobras", "0", "BRL"));
        assertThatThrownBy(() -> service.salvar(novaAcao("PETR4")))
                .isInstanceOf(FalhaDeProvedorException.class);

        prepararCadastro("AAPL");
        when(twelveData.buscarAcao("AAPL")).thenReturn(twelve("Apple", "-1", "USD"));
        assertThatThrownBy(() -> service.salvar(novaAcao("AAPL")))
                .isInstanceOf(FalhaDeProvedorException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void atualizacaoValidaTrocaCotacaoETimestampEmUmaUnicaGravacao() {
        LocalDateTime anterior = LocalDateTime.of(2026, 1, 1, 10, 0);
        Acao acao = acaoExistente("PETR4", "BR", 20.0, anterior);
        when(repository.findById(1L)).thenReturn(Optional.of(acao));
        when(brapi.buscarAcao("PETR4")).thenReturn(brapi("Petrobras", "35.50", "BRL"));
        when(repository.save(acao)).thenReturn(acao);

        Acao atualizada = service.atualizarCotacao(1L);

        assertThat(atualizada.getCotacaoAtual()).isEqualTo(35.50);
        assertThat(atualizada.getDataHoraCotacao()).isAfter(anterior);
        verify(repository).save(acao);
    }

    @Test
    void falhas502E503PreservamCotacaoETimestampAnteriores() {
        verificarFalhaAtomica(new FalhaDeProvedorException("Brapi"));
        verificarFalhaAtomica(new ProvedorIndisponivelException("Brapi"));
    }

    @Test
    void atualizacaoDeAcaoAusenteRetorna404SemConsultarProvedor() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizarCotacao(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(brapi, twelveData);
        verify(repository, never()).save(any());
    }

    @Test
    void buscaPorTickerNormalizaEConsultasAusentesRetornam404() {
        Acao acao = new Acao();
        when(repository.findByTicker("PETR4")).thenReturn(Optional.of(acao));
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.buscarPorTicker(" petr4 ")).isSameAs(acao);
        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository).findByTicker("PETR4");
    }

    private void verificarFalhaAtomica(RuntimeException falha) {
        reset(brapi);
        LocalDateTime anterior = LocalDateTime.of(2026, 1, 1, 10, 0);
        Acao acao = acaoExistente("PETR4", "BR", 20.0, anterior);
        when(repository.findById(1L)).thenReturn(Optional.of(acao));
        when(brapi.buscarAcao("PETR4")).thenThrow(falha);

        assertThatThrownBy(() -> service.atualizarCotacao(1L)).isSameAs(falha);
        assertThat(acao.getCotacaoAtual()).isEqualTo(20.0);
        assertThat(acao.getDataHoraCotacao()).isEqualTo(anterior);
        verify(repository, never()).save(any());
    }

    private void prepararCadastro(String ticker) {
        when(repository.findByTicker(ticker)).thenReturn(Optional.empty());
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(new Corretora()));
        when(repository.save(any(Acao.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private AcaoRequestDTO novaAcao(String ticker) {
        AcaoRequestDTO dto = new AcaoRequestDTO();
        dto.setTicker(ticker);
        dto.setCorretoraId(1L);
        return dto;
    }

    private BrapiResponse brapi(String nome, String cotacao, String moeda) {
        return new BrapiResponse(List.of(new BrapiQuoteResponse(nome, new BigDecimal(cotacao), moeda)));
    }

    private TwelveDataResponse twelve(String nome, String cotacao, String moeda) {
        return new TwelveDataResponse(null, null, null, nome, new BigDecimal(cotacao), moeda);
    }

    private Acao acaoExistente(String ticker, String mercado, double cotacao, LocalDateTime timestamp) {
        Acao acao = new Acao();
        acao.setTicker(ticker);
        acao.setMercado(mercado);
        acao.setCotacaoAtual(cotacao);
        acao.setDataHoraCotacao(timestamp);
        return acao;
    }
}
