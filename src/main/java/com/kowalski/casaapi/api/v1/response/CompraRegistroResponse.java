package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Linha de GET /v1/compras: só os campos da compra, sem carregar parcelas. */
@Schema(description = "Compra, sem as parcelas")
public record CompraRegistroResponse(
        @Schema(description = "Id da compra") UUID id,
        @Schema(description = "Produto", example = "TV 55 POLEGADAS") String nomeProduto,
        @Schema(description = "Valor total", example = "2400.00") BigDecimal valorProduto,
        @Schema(description = "Data da compra", example = "2026-11-15") LocalDate dataCompra,
        @Schema(description = "Quantidade de parcelas", example = "10") Integer numeroParcelas,
        @Schema(description = "Pessoa", example = "PAULO") String nomePessoaCompra,
        @Schema(description = "Cartão", example = "NUBANK") String nomeCartao,
        @Schema(description = "Quando foi cadastrada") LocalDateTime dataCadastro) { }
