package com.kowalski.casaapi.business.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Uma conta em um mês: valor, vencimento e situação de pagamento. */
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

    /** Mês de referência (dia 1) */
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

    /** true depois que a pessoa marca ou desmarca: o débito automático deixa de mexer neste mês */
    @Column(name = "baixa_manual", nullable = false)
    private boolean baixaManual;
}
