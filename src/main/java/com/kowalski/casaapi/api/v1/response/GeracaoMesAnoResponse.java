package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.Month;
import java.time.Year;

@Schema(description = "Geração solar de um mês")
public record GeracaoMesAnoResponse(
        @Schema(description = "Mês (nome em inglês)", example = "NOVEMBER") @JsonSerialize(using = ToStringSerializer.class) Month month,
        @Schema(description = "Ano", example = "2026") @JsonSerialize(using = ToStringSerializer.class) Year year,
        @Schema(description = "Energia gerada no mês (kWh)", example = "612.3") Double value) { }
