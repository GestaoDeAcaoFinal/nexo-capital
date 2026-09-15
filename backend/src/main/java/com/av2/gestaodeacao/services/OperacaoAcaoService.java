package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.cliente.TwelveDataCliente;
import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.OperacaoAcao;
import com.av2.gestaodeacao.domains.dtos.PosicaoCarteiraDTO;
import com.av2.gestaodeacao.domains.dtos.ResumoCarteiraDTO;
import com.av2.gestaodeacao.domains.enums.TipoOperacao;
import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.exceptions.EntradaOuOperacaoInvalidaException;
import com.av2.gestaodeacao.exceptions.RecursoNaoEncontradoException;
import com.av2.gestaodeacao.repositories.AcaoRepository;
import com.av2.gestaodeacao.repositories.OperacaoAcaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OperacaoAcaoService {

    @Autowired
    private AcaoRepository repository;

    @Autowired
    private OperacaoAcaoRepository operacaoRepository;

    @Autowired
    private TwelveDataCliente twelveDataCliente;

    @Transactional
    public OperacaoAcao comprar(Long acaoId, Integer quantidade) {
        return comprar(acaoId, quantidade, null);
    }

    @Transactional
    public OperacaoAcao comprar(Long acaoId, Integer quantidade, Double precoCompra) {
        validarPositivo(quantidade, "Quantidade");
        Acao acao = buscarAcao(acaoId);
        Double precoUnitario = precoCompra == null ? acao.getCotacaoAtual() : precoCompra;
        validarPrecoPositivo(precoUnitario, precoCompra == null
                ? "A cotação atual deve ser positiva"
                : "Preço de compra deve ser positivo");

        OperacaoAcao operacao = new OperacaoAcao();
        operacao.setAcao(acao);
        operacao.setQuantidade(quantidade);
        operacao.setPrecoUnitario(precoUnitario);
        operacao.setTipoOperacao(TipoOperacao.COMPRA);
        operacao.setDataOperacao(LocalDateTime.now());

        List<OperacaoAcao> compras = operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA);
        List<OperacaoAcao> vendas = operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.VENDA);
        PosicaoCalculada posicaoAtual = calcularPosicao(compras, vendas);
        BigDecimal valorNovaCompra = multiplicar(precoUnitario, quantidade);
        operacao.setPrecoMedio(dividir(
                posicaoAtual.custoTotal().add(valorNovaCompra),
                posicaoAtual.quantidade() + quantidade
        ));
        operacao.setValorTotal(valorNovaCompra.doubleValue());
        return operacaoRepository.save(operacao);
    }

    @Transactional
    public OperacaoAcao vender(Long acaoId, Integer quantidade, Double precoVenda) {
        validarPositivo(quantidade, "Quantidade");
        validarPrecoPositivo(precoVenda, "Preço de venda deve ser positivo");

        Acao acao = buscarAcao(acaoId);
        List<OperacaoAcao> compras = operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.COMPRA);
        List<OperacaoAcao> vendas = operacaoRepository.findByAcaoAndTipoOperacao(acao, TipoOperacao.VENDA);
        PosicaoCalculada posicaoAtual = calcularPosicao(compras, vendas);
        if (posicaoAtual.quantidade() == 0) {
            throw new EntradaOuOperacaoInvalidaException("Não existe posição comprada para a ação");
        }

        if (quantidade > posicaoAtual.quantidade()) {
            throw new ConflitoException("Quantidade solicitada excede a posição disponível");
        }

        double precoMedio = dividir(posicaoAtual.custoTotal(), posicaoAtual.quantidade());
        double lucro = BigDecimal.valueOf(precoVenda)
                .subtract(BigDecimal.valueOf(precoMedio))
                .multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        OperacaoAcao venda = new OperacaoAcao();
        venda.setAcao(acao);
        venda.setQuantidade(quantidade);
        venda.setPrecoUnitario(precoVenda);
        venda.setPrecoMedio(precoMedio);
        venda.setLucroPrejuizo(lucro);
        venda.setTipoOperacao(TipoOperacao.VENDA);
        venda.setDataOperacao(LocalDateTime.now());
        venda.setValorTotal(multiplicar(precoVenda, quantidade).doubleValue());
        return operacaoRepository.save(venda);
    }

    public List<OperacaoAcao> listarHistorico(Long acaoId) {
        return operacaoRepository.findByAcao(buscarAcao(acaoId));
    }

    public List<OperacaoAcao> listarCompras() {
        return operacaoRepository.findByTipoOperacao(TipoOperacao.COMPRA);
    }

    public List<OperacaoAcao> listarVendas() {
        return operacaoRepository.findByTipoOperacao(TipoOperacao.VENDA);
    }

    public OperacaoAcao buscarPorId(Long id) {
        return operacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Operação não encontrada"));
    }

    public List<OperacaoAcao> listar() {
        return operacaoRepository.findAll();
    }

    public ResumoCarteiraDTO resumirCarteira() {
        Map<Long, List<OperacaoAcao>> operacoesPorAcao = new LinkedHashMap<>();
        for (OperacaoAcao operacao : operacaoRepository.findAll()) {
            if (operacao.getAcao() != null && operacao.getAcao().getId() != null) {
                operacoesPorAcao
                        .computeIfAbsent(operacao.getAcao().getId(), id -> new java.util.ArrayList<>())
                        .add(operacao);
            }
        }

        Map<String, BigDecimal> taxasCambioParaReal = new LinkedHashMap<>();
        List<PosicaoCarteiraDTO> posicoes = operacoesPorAcao.values().stream()
                .map(operacoes -> criarPosicao(operacoes, taxasCambioParaReal))
                .filter(posicao -> posicao.quantidadeAtual() > 0)
                .sorted(Comparator.comparing(
                        PosicaoCarteiraDTO::ticker,
                        Comparator.nullsLast(String::compareTo)
                ))
                .toList();

        int quantidadeTotal = posicoes.stream().mapToInt(PosicaoCarteiraDTO::quantidadeAtual).sum();
        BigDecimal custoTotal = posicoes.stream()
                .map(posicao -> BigDecimal.valueOf(posicao.custoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorAtual = posicoes.stream()
                .map(posicao -> BigDecimal.valueOf(posicao.valorAtual()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double precoMedioCarteira = quantidadeTotal == 0 ? 0.0 : dividir(custoTotal, quantidadeTotal);

        return new ResumoCarteiraDTO(
                posicoes,
                quantidadeTotal,
                precoMedioCarteira,
                arredondarDinheiro(custoTotal),
                arredondarDinheiro(valorAtual)
        );
    }

    private Acao buscarAcao(Long acaoId) {
        return repository.findById(acaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada"));
    }

    private void validarPositivo(Integer valor, String campo) {
        if (valor == null || valor <= 0) {
            throw new EntradaOuOperacaoInvalidaException(campo + " deve ser positiva");
        }
    }

    private void validarPrecoPositivo(Double valor, String mensagem) {
        if (valor == null || !Double.isFinite(valor) || valor <= 0) {
            throw new EntradaOuOperacaoInvalidaException(mensagem);
        }
    }

    private int quantidadeTotal(List<OperacaoAcao> operacoes) {
        return operacoes.stream().mapToInt(OperacaoAcao::getQuantidade).sum();
    }

    private BigDecimal valorTotal(List<OperacaoAcao> operacoes) {
        return operacoes.stream()
                .map(operacao -> multiplicar(operacao.getPrecoUnitario(), operacao.getQuantidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal custoTotalVendido(List<OperacaoAcao> vendas) {
        return vendas.stream()
                .map(venda -> multiplicar(venda.getPrecoMedio(), venda.getQuantidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PosicaoCarteiraDTO criarPosicao(
            List<OperacaoAcao> operacoes,
            Map<String, BigDecimal> taxasCambioParaReal
    ) {
        Acao acao = operacoes.get(0).getAcao();
        List<OperacaoAcao> compras = operacoes.stream()
                .filter(operacao -> operacao.getTipoOperacao() == TipoOperacao.COMPRA)
                .toList();
        List<OperacaoAcao> vendas = operacoes.stream()
                .filter(operacao -> operacao.getTipoOperacao() == TipoOperacao.VENDA)
                .toList();
        PosicaoCalculada posicao = calcularPosicao(compras, vendas);
        double precoMedio = posicao.quantidade() == 0
                ? 0.0
                : dividir(posicao.custoTotal(), posicao.quantidade());
        BigDecimal taxaCambio = taxaCambioParaReal(acao, taxasCambioParaReal);
        BigDecimal precoMedioEmReais = BigDecimal.valueOf(precoMedio).multiply(taxaCambio);
        BigDecimal custoTotalEmReais = posicao.custoTotal().multiply(taxaCambio);
        BigDecimal cotacaoAtualEmReais = BigDecimal.valueOf(acao.getCotacaoAtual()).multiply(taxaCambio);
        BigDecimal valorAtualEmReais = cotacaoAtualEmReais.multiply(BigDecimal.valueOf(posicao.quantidade()));

        return new PosicaoCarteiraDTO(
                acao.getId(),
                acao.getTicker(),
                posicao.quantidade(),
                arredondarDinheiro(precoMedioEmReais),
                arredondarDinheiro(custoTotalEmReais),
                arredondarDinheiro(cotacaoAtualEmReais),
                arredondarDinheiro(valorAtualEmReais)
        );
    }

    private BigDecimal taxaCambioParaReal(
            Acao acao,
            Map<String, BigDecimal> taxasCambioParaReal
    ) {
        String moeda = acao.getMoeda();
        if (moeda == null || moeda.isBlank() || "BRL".equalsIgnoreCase(moeda)) {
            return BigDecimal.ONE;
        }
        String moedaNormalizada = moeda.trim().toUpperCase(Locale.ROOT);
        return taxasCambioParaReal.computeIfAbsent(
                moedaNormalizada,
                origem -> twelveDataCliente.buscarTaxaCambio(origem, "BRL")
        );
    }

    private PosicaoCalculada calcularPosicao(List<OperacaoAcao> compras, List<OperacaoAcao> vendas) {
        int quantidade = quantidadeTotal(compras) - quantidadeTotal(vendas);
        BigDecimal custoTotal = valorTotal(compras).subtract(custoTotalVendido(vendas));
        if (quantidade == 0) {
            custoTotal = BigDecimal.ZERO;
        }
        return new PosicaoCalculada(quantidade, custoTotal);
    }

    private double arredondarDinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal multiplicar(Double preco, Integer quantidade) {
        return BigDecimal.valueOf(preco).multiply(BigDecimal.valueOf(quantidade));
    }

    private double dividir(BigDecimal valor, int quantidade) {
        return valor.divide(BigDecimal.valueOf(quantidade), 10, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record PosicaoCalculada(int quantidade, BigDecimal custoTotal) {
    }
}
