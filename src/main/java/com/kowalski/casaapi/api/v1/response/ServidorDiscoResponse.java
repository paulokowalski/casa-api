package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Espaço de um disco")
public record ServidorDiscoResponse(
        @Schema(description = "Nome configurado em monitor.discos", example = "NVMe") String nome,
        @Schema(description = "Caminho medido dentro do container", example = "/") String caminho,
        @Schema(description = "Tamanho total (bytes)", example = "1006979764224") long total,
        @Schema(description = "Espaço usado (bytes)", example = "106906300416") long usado,
        @Schema(description = "Percentual usado, como no df", example = "10.6") double pct) { }
