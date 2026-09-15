package com.av2.gestaodeacao.resources;

import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.dtos.CorretoraRequestDTO;
import com.av2.gestaodeacao.services.CorretoraService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/corretoras")
@Validated
public class CorretoraResource {

    @Autowired
    private CorretoraService service;

    @PostMapping
    public Corretora salvar(@Valid @RequestBody CorretoraRequestDTO dto) {
        return service.salvar(dto);
    }

    @GetMapping
    public List<Corretora> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Corretora buscarPorId(@PathVariable @Positive(message = "Id deve ser positivo") Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/cnpj/{cnpj}")
    public Corretora buscarPorCnpj(
            @PathVariable
            @Pattern(
                    regexp = "^\\s*(?:\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})\\s*$",
                    message = "CNPJ possui formato inválido"
            ) String cnpj
    ) {
        return service.buscarPorCnpj(cnpj);
    }
}
