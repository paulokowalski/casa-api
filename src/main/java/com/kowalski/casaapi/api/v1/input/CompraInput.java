package com.kowalski.casaapi.api.v1.input;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dados para cadastrar ou editar uma compra")
public record CompraInput(
        @Schema(description = "Descrição do produto (gravada em maiúsculas)", example = "TV 55 POLEGADAS") String nomeProduto,
        @Schema(description = "Valor total da compra; negativo para estorno/crédito", example = "2400.00") BigDecimal valorProduto,
        @Schema(description = "Data da compra (aaaa-mm-dd); a 1ª parcela vence no mês seguinte", example = "2026-11-15") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dataCompra,
        @Schema(description = "Quantidade de parcelas (1 = à vista)", example = "10") Integer numeroParcelas,
        @Schema(description = "Pessoa ou pessoas (separadas por vírgula) que dividem a compra", example = "PAULO") String nomePessoaCompra,
        @Schema(description = "Id do tipo de lançamento (cartão); ver GET /v1/filtros/tipos-lancamento") UUID tipoLancamentoId,
        @Schema(description = "Id da categoria (opcional); ver GET /v1/filtros/categorias") UUID categoriaId) {

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