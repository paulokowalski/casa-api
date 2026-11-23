package com.kowalski.casaapi.conta;

import jakarta.persistence.*;
import org.springframework.util.Assert;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
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

    public void registrarValorReal(BigDecimal valorReal) {
        Assert.isTrue(valorReal.signum() > 0, "O valor deve ser maior que zero");
        this.valor = valorReal;
        this.valorEstimado = false;
    }

    public void marcarPaga(boolean pagaAgora, LocalDate dataInformada, LocalDate hoje) {
        this.paga = pagaAgora;
        this.baixaManual = true;
        this.dataPagamento = pagaAgora ? Objects.requireNonNullElse(dataInformada, hoje) : null;
    }

    public void corrigirDataDePagamento(LocalDate data) {
        if (paga) this.dataPagamento = data;
    }

    public boolean baixaAutomaticaPendenteEm(LocalDate hoje) {
        return conta.isDebitoAutomatico() && !paga && !baixaManual && !vencimento.isAfter(hoje);
    }

    public void baixarNoVencimento() {
        this.paga = true;
        this.dataPagamento = vencimento;
    }
}
