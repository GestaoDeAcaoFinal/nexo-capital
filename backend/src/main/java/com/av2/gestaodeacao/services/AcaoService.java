package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.cliente.AcaoCliente;
import com.av2.gestaodeacao.cliente.TwelveDataCliente;
import com.av2.gestaodeacao.cliente.dto.BrapiQuoteResponse;
import com.av2.gestaodeacao.cliente.dto.TwelveDataResponse;
import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.dtos.AcaoRequestDTO;
import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.exceptions.FalhaDeProvedorException;
import com.av2.gestaodeacao.exceptions.RecursoNaoEncontradoException;
import com.av2.gestaodeacao.repositories.AcaoRepository;
import com.av2.gestaodeacao.repositories.CorretoraRepository;
import com.av2.gestaodeacao.validation.TickerNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AcaoService {

    @Autowired private AcaoRepository repository;
    @Autowired private CorretoraRepository corretoraRepository;
    @Autowired private AcaoCliente cliente;
    @Autowired private TwelveDataCliente twelveDataCliente;
    @Autowired private MercadoTickerClassifier mercadoTickerClassifier;

    public Acao salvar(AcaoRequestDTO dto) {
        String ticker = TickerNormalizer.normalizar(dto.getTicker());
        if (repository.findByTicker(ticker).isPresent()) {
            throw new ConflitoException("Ticker já cadastrado");
        }
        Corretora corretora = corretoraRepository.findById(dto.getCorretoraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada"));
        DadosMercado dados = buscarDadosMercado(ticker);
        validarDadosCompletos(dados);

        Acao acao = new Acao();
        acao.setTicker(ticker);
        acao.setCorretoraRelacionada(corretora);
        acao.setNomeEmpresa(dados.nomeEmpresa());
        acao.setCotacaoAtual(dados.cotacao().doubleValue());
        acao.setMoeda(dados.moeda());
        acao.setMercado(dados.mercado());
        acao.setDataHoraCotacao(LocalDateTime.now());
        try {
            Acao acaoSalva = repository.save(acao);
            repository.flush();
            return acaoSalva;
        } catch (DataIntegrityViolationException exception) {
            throw new ConflitoException("Ticker já cadastrado");
        }
    }

    public List<Acao> listar() {
        return repository.findAll();
    }

    public Acao buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada"));
    }

    public Acao buscarPorTicker(String ticker) {
        return repository.findByTicker(TickerNormalizer.normalizar(ticker))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada"));
    }

    public Acao atualizarCotacao(Long id) {
        Acao acao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada"));

        BigDecimal novaCotacao = buscarCotacao(acao);
        validarCotacaoPositiva(novaCotacao, provedorDoMercado(acao.getMercado()));
        LocalDateTime novoInstante = LocalDateTime.now();

        acao.setCotacaoAtual(novaCotacao.doubleValue());
        acao.setDataHoraCotacao(novoInstante);
        return repository.save(acao);
    }

    private DadosMercado buscarDadosMercado(String ticker) {
        String mercado = mercadoTickerClassifier.classificar(ticker);
        if ("EUA".equals(mercado)) {
            TwelveDataResponse response = twelveDataCliente.buscarAcao(ticker);
            return new DadosMercado(response.name(), response.close(), response.currency(), mercado, "Twelve Data");
        }
        BrapiQuoteResponse response = cliente.buscarAcao(ticker).results().get(0);
        return new DadosMercado(
                response.longName(), response.regularMarketPrice(), response.currency(), mercado, "Brapi");
    }

    private BigDecimal buscarCotacao(Acao acao) {
        if ("EUA".equals(acao.getMercado())) {
            return twelveDataCliente.buscarAcao(acao.getTicker()).close();
        }
        return cliente.buscarAcao(acao.getTicker()).results().get(0).regularMarketPrice();
    }

    private void validarDadosCompletos(DadosMercado dados) {
        if (vazio(dados.nomeEmpresa()) || vazio(dados.moeda())) {
            throw new FalhaDeProvedorException(dados.provedor());
        }
        validarCotacaoPositiva(dados.cotacao(), dados.provedor());
    }

    private void validarCotacaoPositiva(BigDecimal cotacao, String provedor) {
        if (cotacao == null || cotacao.signum() <= 0) {
            throw new FalhaDeProvedorException(provedor);
        }
    }

    private String provedorDoMercado(String mercado) {
        return "EUA".equals(mercado) ? "Twelve Data" : "Brapi";
    }

    private boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private record DadosMercado(
            String nomeEmpresa,
            BigDecimal cotacao,
            String moeda,
            String mercado,
            String provedor
    ) {
    }
}
