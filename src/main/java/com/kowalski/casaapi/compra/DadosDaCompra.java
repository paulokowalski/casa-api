package com.kowalski.casaapi.compra;

import com.kowalski.casaapi.categoria.Categorias;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosDaCompra(String nomeProduto, BigDecimal valorProduto, LocalDate dataCompra, Integer numeroParcelas,
                            String nomePessoa, TipoLancamento cartao, Categorias categoria) {
}
