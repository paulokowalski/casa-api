package com.kowalski.casaapi.business.model;

import com.kowalski.casaapi.business.enumerator.TipoConta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conta")
@Getter
@Setter
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categorias categoria;

    @Column(nullable = false, length = 120)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoConta tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "dia_vencimento", nullable = false)
    private Integer diaVencimento;

    @Column(name = "competencia_inicio", nullable = false)
    private LocalDate competenciaInicio;

    @Column(name = "competencia_fim")
    private LocalDate competenciaFim;

    @Column(name = "parcela_inicial")
    private Integer parcelaInicial;

    @Column(name = "total_parcelas")
    private Integer totalParcelas;

    @Column(name = "juros_mes", precision = 7, scale = 4)
    private BigDecimal jurosMes;

    @Column(name = "valor_variavel", nullable = false)
    private boolean valorVariavel;

    @Column(name = "debito_automatico", nullable = false)
    private boolean debitoAutomatico;

    @Column(name = "dt_cadastro", nullable = false)
    private LocalDateTime dataCadastro;

    @PrePersist
    void aoCriar() {
        if (dataCadastro == null) dataCadastro = LocalDateTime.now();
    }
}
