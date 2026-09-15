package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.cliente.CnpjCliente;
import com.av2.gestaodeacao.cliente.ViaCepCliente;
import com.av2.gestaodeacao.cliente.dto.BrasilApiCnpjResponse;
import com.av2.gestaodeacao.cliente.dto.ViaCepResponse;
import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.dtos.CorretoraRequestDTO;
import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.ProvedorIndisponivelException;
import com.av2.gestaodeacao.exceptions.RecursoNaoEncontradoException;
import com.av2.gestaodeacao.repositories.CorretoraRepository;
import com.av2.gestaodeacao.services.cvm.CadastroCvmCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CorretoraServiceTest {

    private CorretoraRepository repository;
    private ViaCepCliente viaCepCliente;
    private CnpjCliente cnpjCliente;
    private CadastroCvmCache cadastroCvmCache;
    private CorretoraService service;

    @BeforeEach
    void configurar() {
        repository = mock(CorretoraRepository.class);
        viaCepCliente = mock(ViaCepCliente.class);
        cnpjCliente = mock(CnpjCliente.class);
        cadastroCvmCache = mock(CadastroCvmCache.class);
        service = new CorretoraService();
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "viaCepCliente", viaCepCliente);
        ReflectionTestUtils.setField(service, "cnpjCliente", cnpjCliente);
        ReflectionTestUtils.setField(service, "cadastroCvmCache", cadastroCvmCache);
    }

    @Test
    void cadastraSomenteAposEnriquecimentoEConfirmacaoAtivaNaCvm() {
        prepararEnriquecimentoValido();
        when(cadastroCvmCache.intermediarioAtivo("12345678000190")).thenReturn(true);
        when(repository.save(any(Corretora.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Corretora salva = service.salvar(novaCorretora());

        assertThat(salva.getCnpj()).isEqualTo("12345678000190");
        assertThat(salva.getCep()).isEqualTo("01001000");
        assertThat(salva.getRazaoSocial()).isEqualTo("Corretora SA");
        assertThat(salva.getCidade()).isEqualTo("São Paulo");
        assertThat(salva.getValidadaNaCvm()).isTrue();
        assertThat(salva.getDataCadastro()).isNotNull();
        verify(repository).findByCnpj("12345678000190");
        verify(viaCepCliente).buscarCep("01001000");
        verify(cnpjCliente).buscarCnpj("12345678000190");
        verify(cadastroCvmCache).intermediarioAtivo("12345678000190");
        verify(repository).save(salva);
    }

    @Test
    void duplicidadeNormalizadaInterrompeAntesDeProvedores() {
        when(repository.findByCnpj("12345678000190")).thenReturn(Optional.of(new Corretora()));

        assertThatThrownBy(() -> service.salvar(novaCorretora()))
                .isInstanceOf(ConflitoException.class);

        verifyNoInteractions(viaCepCliente, cnpjCliente, cadastroCvmCache);
        verify(repository, never()).save(any());
    }

    @Test
    void violacaoConcorrenteDeCnpjEhTraduzidaParaConflito() {
        prepararEnriquecimentoValido();
        when(cadastroCvmCache.intermediarioAtivo("12345678000190")).thenReturn(true);
        when(repository.save(any(Corretora.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new DataIntegrityViolationException("uk_corretora_cnpj"))
                .when(repository).flush();

        assertThatThrownBy(() -> service.salvar(novaCorretora()))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("CNPJ já cadastrado");
    }

    @Test
    void intermediarioAusenteOuInativoNaoEhPersistidoComoValidado() {
        prepararEnriquecimentoValido();
        when(cadastroCvmCache.intermediarioAtivo("12345678000190")).thenReturn(false);

        assertThatThrownBy(() -> service.salvar(novaCorretora()))
                .isInstanceOf(DadoExternoNaoEncontradoException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void indisponibilidadeDaCvmNaoPresumeValidacaoNemPersiste() {
        prepararEnriquecimentoValido();
        when(cadastroCvmCache.intermediarioAtivo("12345678000190"))
                .thenThrow(new ProvedorIndisponivelException("CVM"));

        assertThatThrownBy(() -> service.salvar(novaCorretora()))
                .isInstanceOf(ProvedorIndisponivelException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void falhaNoEnriquecimentoInterrompeOFluxoSemGravacaoParcial() {
        when(repository.findByCnpj("12345678000190")).thenReturn(Optional.empty());
        when(viaCepCliente.buscarCep("01001000"))
                .thenThrow(new DadoExternoNaoEncontradoException("CEP não encontrado"));

        assertThatThrownBy(() -> service.salvar(novaCorretora()))
                .isInstanceOf(DadoExternoNaoEncontradoException.class);

        verifyNoInteractions(cnpjCliente, cadastroCvmCache);
        verify(repository, never()).save(any());
    }

    @Test
    void buscaPorCnpjNormalizaEListaSemTransformarResultado() {
        Corretora corretora = new Corretora();
        when(repository.findByCnpj("12345678000190")).thenReturn(Optional.of(corretora));
        when(repository.findAll()).thenReturn(List.of(corretora));

        assertThat(service.buscarPorCnpj(" 12.345.678/0001-90 ")).isSameAs(corretora);
        assertThat(service.listar()).containsExactly(corretora);
        verify(repository).findByCnpj("12345678000190");
    }

    @Test
    void buscasAusentesUsamRecursoNaoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        when(repository.findByCnpj("12345678000190")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.buscarPorCnpj("12345678000190"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private void prepararEnriquecimentoValido() {
        when(repository.findByCnpj("12345678000190")).thenReturn(Optional.empty());
        when(viaCepCliente.buscarCep("01001000"))
                .thenReturn(new ViaCepResponse(false, "Praça da Sé", "Sé", "São Paulo", "SP"));
        when(cnpjCliente.buscarCnpj("12345678000190"))
                .thenReturn(new BrasilApiCnpjResponse(
                        "Corretora SA", "Corretora", "contato@corretora.test", "1133334444", "ATIVA"));
    }

    private CorretoraRequestDTO novaCorretora() {
        CorretoraRequestDTO dto = new CorretoraRequestDTO();
        dto.setCnpj(" 12.345.678/0001-90 ");
        dto.setCep("01001-000");
        return dto;
    }
}
