package com.av2.gestaodeacao.services;

import com.av2.gestaodeacao.cliente.CnpjCliente;
import com.av2.gestaodeacao.cliente.ViaCepCliente;
import com.av2.gestaodeacao.cliente.dto.BrasilApiCnpjResponse;
import com.av2.gestaodeacao.cliente.dto.ViaCepResponse;
import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.dtos.CorretoraRequestDTO;
import com.av2.gestaodeacao.exceptions.ConflitoException;
import com.av2.gestaodeacao.exceptions.DadoExternoNaoEncontradoException;
import com.av2.gestaodeacao.exceptions.RecursoNaoEncontradoException;
import com.av2.gestaodeacao.repositories.CorretoraRepository;
import com.av2.gestaodeacao.services.cvm.CadastroCvmCache;
import com.av2.gestaodeacao.validation.CepNormalizer;
import com.av2.gestaodeacao.validation.CnpjNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CorretoraService {
    @Autowired private CorretoraRepository repository;
    @Autowired private ViaCepCliente viaCepCliente;
    @Autowired private CnpjCliente cnpjCliente;
    @Autowired private CadastroCvmCache cadastroCvmCache;

    public Corretora salvar(CorretoraRequestDTO dto) {
        Corretora corretora = new Corretora();
        corretora.setCnpj(CnpjNormalizer.normalizar(dto.getCnpj()));
        corretora.setCep(CepNormalizer.normalizar(dto.getCep()));
        if (repository.findByCnpj(corretora.getCnpj()).isPresent()) {
            throw new ConflitoException("CNPJ já cadastrado");
        }
        preencherEndereco(corretora);
        preencherDadosCadastrais(corretora);
        if (!cadastroCvmCache.intermediarioAtivo(corretora.getCnpj())) {
            throw new DadoExternoNaoEncontradoException(
                    "CNPJ não consta como intermediário ativo na CVM");
        }
        corretora.setValidadaNaCvm(true);
        corretora.setDataCadastro(LocalDateTime.now());
        try {
            Corretora corretoraSalva = repository.save(corretora);
            repository.flush();
            return corretoraSalva;
        } catch (DataIntegrityViolationException exception) {
            throw new ConflitoException("CNPJ já cadastrado");
        }
    }

    public List<Corretora> listar() { return repository.findAll(); }

    public Corretora buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada"));
    }

    public Corretora buscarPorCnpj(String cnpj) {
        return repository.findByCnpj(CnpjNormalizer.normalizar(cnpj))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada"));
    }

    private void preencherEndereco(Corretora corretora) {
        ViaCepResponse cepData = viaCepCliente.buscarCep(corretora.getCep());
        corretora.setLogradouro(cepData.logradouro());
        corretora.setBairro(cepData.bairro());
        corretora.setCidade(cepData.localidade());
        corretora.setUf(cepData.uf());
    }

    private void preencherDadosCadastrais(Corretora corretora) {
        BrasilApiCnpjResponse dados = cnpjCliente.buscarCnpj(corretora.getCnpj());
        corretora.setRazaoSocial(dados.razaoSocial());
        corretora.setNomeFantasia(dados.nomeFantasia());
        corretora.setEmail(dados.email());
        corretora.setTelefone(dados.telefone());
        corretora.setSituacaoCadastral(dados.situacaoCadastral());
    }
}
