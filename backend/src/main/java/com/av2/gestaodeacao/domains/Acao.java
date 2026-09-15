package com.av2.gestaodeacao.domains;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "acoes",
        uniqueConstraints = @UniqueConstraint(name = "uk_acoes_ticker", columnNames = "ticker")
)
public class Acao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticker;
    private String nomeEmpresa;
    private String mercado;
    private String moeda;
    private Double cotacaoAtual;
    private LocalDateTime dataHoraCotacao;

    @ManyToOne
    @JoinColumn(name = "corretora_id")
    private Corretora corretoraRelacionada;
}
