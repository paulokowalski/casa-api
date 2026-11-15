package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Situação da fatura no mês")
public record FaturaResponse(
        @Schema(description = "Mês no formato aaaa-mm", example = "2026-11") String competencia,
        @Schema(description = "Fatura paga", example = "true") boolean paga,
        @Schema(description = "Data do pagamento", example = "2026-11-10") LocalDate dataPagamento) {
}
