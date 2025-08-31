package com.kowalski.casaapi.api.v1.response;

import com.kowalski.casaapi.business.model.Categorias;

import java.time.LocalDate;

public record CompraParcelaResponse(
        String id,
        String nomeCompra,
        Double valorParcela,
        LocalDate dataParcela,
        LocalDate dataCompra,
        int numeroTotalParcela,
        int numeroParcela,
        String ultimaParcela,
        String nomeCartao,
        Double valorFaltante,
        String nomePessoa,
        Double valorTotal,
        String categoria) { }
