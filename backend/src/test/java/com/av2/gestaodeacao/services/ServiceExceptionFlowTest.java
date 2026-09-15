package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.cliente.AcaoCliente;
import com.av2.gestaodeacao.cliente.TwelveDataCliente;
import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.dtos.AcaoRequestDTO;
import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import com.av2.gestaodeacao.exceptions.RecursoNaoEncontradoException;
import com.av2.gestaodeacao.repositories.AcaoRepository;
import com.av2.gestaodeacao.repositories.CorretoraRepository;
import com.av2.gestaodeacao.repositories.OperacaoAcaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ServiceExceptionFlowTest {

    private AcaoRepository acaoRepository;
    private CorretoraRepository corretoraRepository;
    private OperacaoAcaoRepository operacaoRepository;
    private AcaoCliente acaoCliente;
    private TwelveDataCliente twelveDataCliente;
    private AcaoService acaoService;
    private OperacaoAcaoService operacaoService;

    @BeforeEach
    void configurarServicos() {
        acaoRepository = mock(AcaoRepository.class);
        corretoraRepository = mock(CorretoraRepository.class);
        operacaoRepository = mock(OperacaoAcaoRepository.class);
        acaoCliente = mock(AcaoCliente.class);
        twelveDataCliente = mock(TwelveDataCliente.class);

        acaoService = new AcaoService();
        ReflectionTestUtils.setField(acaoService, "repository", acaoRepository);
        ReflectionTestUtils.setField(acaoService, "corretoraRepository", corretoraRepository);
        ReflectionTestUtils.setField(acaoService, "cliente", acaoCliente);
        ReflectionTestUtils.setField(acaoService, "twelveDataCliente", twelveDataCliente);
        ReflectionTestUtils.setField(acaoService, "mercadoTickerClassifier", new MercadoTickerClassifier());

        operacaoService = new OperacaoAcaoService();
        ReflectionTestUtils.setField(operacaoService, "repository", acaoRepository);
        ReflectionTestUtils.setField(operacaoService, "operacaoRepository", operacaoRepository);
    }

    @Test
    void operacaoInvalidaFalhaAntesDeConsultarPersistencia() {
        assertThatThrownBy(() -> operacaoService.comprar(1L, 0))
                .isInstanceOf(EntradaOuOperacaoInvalidaException.class);

        verifyNoInteractions(acaoRepository, operacaoRepository);
    }

    @Test
    void recursoLocalAusenteUsaExcecaoEspecifica() {
        when(acaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> acaoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void tickerDuplicadoNormalizadoUsaConflitoSemConsultarProvedor() {
        Acao existente = new Acao();
        when(acaoRepository.findByTicker("PETR4")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> acaoService.salvar(novaAcao(" petr4 ")))
                .isInstanceOf(ConflitoException.class);

        verifyNoInteractions(corretoraRepository, acaoCliente, twelveDataCliente);
        verify(acaoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tickerBemFormadoAusenteNoProvedorUsaDadoExternoNaoEncontrado() {
        prepararCadastroAmericano();
        when(twelveDataCliente.buscarAcao("AAPL"))
                .thenThrow(new DadoExternoNaoEncontradoException("Ticker não encontrado"));

        assertThatThrownBy(() -> acaoService.salvar(novaAcao("aapl")))
                .isInstanceOf(DadoExternoNaoEncontradoException.class);

        verify(acaoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void erroDoProvedorUsaFalhaDeProvedorSemPersistir() {
        prepararCadastroAmericano();
        when(twelveDataCliente.buscarAcao("AAPL"))
                .thenThrow(new FalhaDeProvedorException("Twelve Data",
                        new IllegalStateException("resposta externa secreta")));

        assertThatThrownBy(() -> acaoService.salvar(novaAcao("AAPL")))
                .isInstanceOf(FalhaDeProvedorException.class)
                .hasMessageNotContaining("secreta");

        verify(acaoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void timeoutDoProvedorUsaIndisponibilidadeSemPersistir() {
        prepararCadastroAmericano();
        when(twelveDataCliente.buscarAcao("AAPL"))
                .thenThrow(new ProvedorIndisponivelException("Twelve Data",
                        new IllegalStateException("url?apikey=secreta")));

        assertThatThrownBy(() -> acaoService.salvar(novaAcao("AAPL")))
                .isInstanceOf(ProvedorIndisponivelException.class)
                .hasMessageNotContaining("apikey");

        verify(acaoRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private void prepararCadastroAmericano() {
        when(acaoRepository.findByTicker("AAPL")).thenReturn(Optional.empty());
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(new Corretora()));
    }

    private AcaoRequestDTO novaAcao(String ticker) {
        AcaoRequestDTO dto = new AcaoRequestDTO();
        dto.setTicker(ticker);
        dto.setCorretoraId(1L);
        return dto;
    }
}
