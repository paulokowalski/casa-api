package com.kowalski.casaapi.api.v1.input;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraInput(
        String nomeProduto,
        BigDecimal valorProduto,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataCompra,
        Integer numeroParcelas,
        String nomePessoaCompra,
        String nomeCartao) {
}