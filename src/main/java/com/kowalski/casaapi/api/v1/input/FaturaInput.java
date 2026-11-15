package com.kowalski.casaapi.api.v1.input;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Situação da fatura")
public record FaturaInput(
        @Schema(description = "Fatura paga", example = "true") boolean paga,
        @Schema(description = "Data do pagamento (aaaa-mm-dd)", example = "2026-11-10") LocalDate dataPagamento) {
}
