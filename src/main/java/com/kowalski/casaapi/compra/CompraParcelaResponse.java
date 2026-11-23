package com.kowalski.casaapi.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Uma parcela de compra num mês")
public record CompraParcelaResponse(
        @Schema(description = "Id da compra (não da parcela)") String id,
        @Schema(description = "Produto", example = "TV 55 POLEGADAS") String nomeCompra,
        @Schema(description = "Valor desta parcela", example = "240.00") Double valorParcela,
        @Schema(description = "Mês de vencimento da parcela (aaaa-mm-dd)", example = "2026-12-15") LocalDate dataParcela,
        @Schema(description = "Data da compra", example = "2026-11-15") LocalDate dataCompra,
        @Schema(description = "Total de parcelas da compra", example = "10") int numeroTotalParcela,
        @Schema(description = "Número desta parcela", example = "1") int numeroParcela,
        @Schema(description = "SIM se for a última parcela, senão NÃO", example = "NÃO") String ultimaParcela,
        @Schema(description = "Cartão (tipo de lançamento)", example = "NUBANK") String nomeCartao,
        @Schema(description = "Quanto ainda falta pagar depois desta parcela", example = "2160.00") Double valorFaltante,
        @Schema(description = "Pessoa", example = "PAULO") String nomePessoa,
        @Schema(description = "Valor total da compra", example = "2400.00") Double valorTotal,
        @Schema(description = "Categoria; vazio se não tiver", example = "CASA") String categoria) { }
