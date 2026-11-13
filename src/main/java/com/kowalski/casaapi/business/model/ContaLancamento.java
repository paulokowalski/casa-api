package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "conta_lancamento", uniqueConstraints = @UniqueConstraint(name = "uq_lancamento_mes", columnNames = {"conta_id", "competencia"}))
@Getter
@Setter
public class ContaLancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @Column(nullable = false)
    private LocalDate competencia;

    @Column(name = "numero_parcela")
    private Integer numeroParcela;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "valor_estimado", nullable = false)
    private boolean valorEstimado;

    @Column(nullable = false)
    private boolean paga;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "baixa_manual", nullable = false)
    private boolean baixaManual;
}
