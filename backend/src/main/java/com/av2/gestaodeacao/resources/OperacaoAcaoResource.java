package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.OperacaoAcao;
import com.av2.gestaodeacao.domains.dtos.ResumoCarteiraDTO;
import com.av2.gestaodeacao.services.OperacaoAcaoService;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/operacoes")
@Validated
public class OperacaoAcaoResource {

    @Autowired
    private OperacaoAcaoService service;

    @GetMapping
    public List<OperacaoAcao> listar() {

        return service.listar();
    }

    @GetMapping("/carteira")
    public ResumoCarteiraDTO resumirCarteira() {
        return service.resumirCarteira();
    }

    @PostMapping("/comprar/{acaoId}")
    public OperacaoAcao comprar(
            @PathVariable @Positive(message = "Id da ação deve ser positivo") Long acaoId,
            @RequestParam @Positive(message = "Quantidade deve ser positiva") Integer quantidade,
            @RequestParam(required = false)
            @Positive(message = "Preço de compra deve ser positivo") Double precoCompra
    ) {

        return service.comprar(
                acaoId,
                quantidade,
                precoCompra
        );
    }

    @PostMapping("/vender/{acaoId}")
    public OperacaoAcao vender(
            @PathVariable @Positive(message = "Id da ação deve ser positivo") Long acaoId,
            @RequestParam @Positive(message = "Quantidade deve ser positiva") Integer quantidade,
            @RequestParam @Positive(message = "Preço de venda deve ser positivo") Double precoVenda
    ) {

        return service.vender(
                acaoId,
                quantidade,
                precoVenda
        );
    }

    @GetMapping("/{acaoId}")
    public List<OperacaoAcao> listar(
            @PathVariable @Positive(message = "Id da ação deve ser positivo") Long acaoId
    ) {

        return service.listarHistorico(acaoId);
    }

    @GetMapping("/compras")
    public List<OperacaoAcao> listarCompras() {

        return service.listarCompras();
    }

    @GetMapping("/vendas")
    public List<OperacaoAcao> listarVendas() {

        return service.listarVendas();
    }

    @GetMapping("/buscar/{id}")
    public OperacaoAcao buscarPorId(
            @PathVariable @Positive(message = "Id deve ser positivo") Long id
    ) {

        return service.buscarPorId(id);
    }
}
