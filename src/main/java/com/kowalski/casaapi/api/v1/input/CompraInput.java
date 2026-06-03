package com.kowalski.casaapi.api.v1.input;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CompraInput(
        String nomeProduto,
        BigDecimal valorProduto,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dataCompra,
        Integer numeroParcelas,
        String nomePessoaCompra,
        UUID tipoLancamentoId,
        UUID categoriaId) {

        public CompraInput comNomePessoaEValor(String nomePessoa, BigDecimal valor) {
                return new CompraInput(
                        this.nomeProduto,
                        valor,
                        this.dataCompra,
                        this.numeroParcelas,
                        nomePessoa,
                        this.tipoLancamentoId,
                        this.categoriaId
                );
        }
}