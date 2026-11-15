package com.kowalski.casaapi.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Schema(description = "Leitura enviada pelo inversor solar")
public class GeradoDTO {

    @Schema(description = "Energia gerada no dia até agora (kWh)", example = "12.4")
    private Double gerado;

    @Schema(description = "Potência instantânea (W)", example = "3120")
    private Double potencia;

}