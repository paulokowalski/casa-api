package com.kowalski.casaapi.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter @Setter
public class Transacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;
    private String descricao;
    private BigDecimal valor;
    private LocalDate data;
    private Boolean fixa;
    private Integer ano;
    private Integer mes;

    // NOVO: identificador da série de recorrência
    private String idSerie;

    @ManyToOne
    @JoinColumn(name = "pessoa_id")
    private Pessoa pessoa;

    // getters e setters
}