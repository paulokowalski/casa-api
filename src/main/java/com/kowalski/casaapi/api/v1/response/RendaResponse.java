package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Renda do mês")
public record RendaResponse(
        @Schema(description = "Mês no formato aaaa-mm", example = "2026-11") String competencia,
        @Schema(description = "Valor da renda; nulo se nunca foi informada", example = "8500.00") BigDecimal valor,
        @Schema(description = "Mês (aaaa-mm) de onde o valor veio", example = "2026-03") String origem,
        @Schema(description = "true se o valor veio de um mês anterior", example = "true") boolean herdada) {
}
