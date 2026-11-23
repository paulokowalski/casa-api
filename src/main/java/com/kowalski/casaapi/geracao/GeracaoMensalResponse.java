package com.kowalski.casaapi.geracao;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Month;
import java.time.Year;
import java.util.List;

@Schema(description = "Geração solar por dia no mês")
public record GeracaoMensalResponse(
        @Schema(description = "Energia gerada (kWh) em cada dia") List<GeracaoResponse> valores,
        @Schema(description = "Ano", example = "2026") @JsonSerialize(using = ToStringSerializer.class) Year ano,
        @Schema(description = "Mês (nome em inglês)", example = "NOVEMBER") @JsonSerialize(using = ToStringSerializer.class) Month mes) { }
