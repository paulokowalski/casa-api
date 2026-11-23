package com.kowalski.casaapi.fatura;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

import static com.kowalski.casaapi.comum.Competencias.formatar;

@Schema(description = "Situação da fatura no mês")
public record FaturaResponse(
        @Schema(description = "Mês no formato aaaa-mm", example = "2026-11") String competencia,
        @Schema(description = "Fatura paga", example = "true") boolean paga,
        @Schema(description = "Data do pagamento", example = "2026-11-10") LocalDate dataPagamento) {

    public static FaturaResponse paga(LocalDate competencia, LocalDate dataPagamento) {
        return new FaturaResponse(formatar(competencia), true, dataPagamento);
    }

    public static FaturaResponse naoPaga(LocalDate competencia) {
        return new FaturaResponse(formatar(competencia), false, null);
    }
}
