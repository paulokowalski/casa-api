package com.kowalski.casaapi.api.v1.input;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompraInput(
        String nomeProduto,
        BigDecimal valorProduto,
        LocalDate dataCompra,
        Integer numeroParcelas,
        String nomePessoaCompra,
        String nomeCartao) {
}