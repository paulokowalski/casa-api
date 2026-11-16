package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Estatísticas de um dia")
public record ServidorResumoDiaResponse(
        @Schema(description = "Dia", example = "2026-11-15") LocalDate data,
        @Schema(description = "Estatísticas do dia") ServidorEstatisticasResponse resumo,
        @Schema(description = "Resumo do host no dia; nulo sem amostras do host") ServidorHostDiaResponse.Resumo host) {

    public ServidorResumoDiaResponse(LocalDate data, ServidorEstatisticasResponse resumo) {
        this(data, resumo, null);
    }
}
