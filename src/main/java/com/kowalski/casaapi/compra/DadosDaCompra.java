package com.kowalski.casaapi.compra;

import com.kowalski.casaapi.categoria.Categorias;
import com.kowalski.casaapi.pessoa.Pessoa;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DadosDaCompra(String nomeProduto, BigDecimal valorProduto, LocalDate dataCompra, Integer numeroParcelas,
                            Pessoa pessoa, TipoLancamento cartao, Categorias categoria) {
}
