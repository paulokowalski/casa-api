package com.kowalski.casaapi.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class TransacaoDTO {
    private String tipo; // 'despesa' ou 'receita'
    private String descricao;
    private BigDecimal valor;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate data;
    private Boolean fixa;
    private Integer ano;
    private Integer mes;
    private Long pessoa; // apenas o id da pessoa
    private String idSerie;
    private Boolean paga = false;
}