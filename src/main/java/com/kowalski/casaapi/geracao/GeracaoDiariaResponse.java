package com.kowalski.casaapi.geracao;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Geração solar de um dia")
public record GeracaoDiariaResponse(
        @Schema(description = "Leituras de potência (W) ao longo do dia") List<GeracaoResponse> valores,
        @Schema(description = "Energia gerada no dia (kWh)", example = "24.6") Double geracao) { }
