package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Totais do cartão num mês (nulos quando não há parcelas)")
public record DespesaResponse(
        @Schema(description = "Soma de todas as parcelas do mês", example = "3250.40") BigDecimal valorMes,
        @Schema(description = "Parte que vem de compras parceladas", example = "2100.00") BigDecimal comprasParceladas,
        @Schema(description = "Parte que vem de compras à vista (sem estornos)", example = "1150.40") BigDecimal gastosNoCartao) { }