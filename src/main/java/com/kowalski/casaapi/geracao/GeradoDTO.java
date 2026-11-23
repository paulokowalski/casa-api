package com.kowalski.casaapi.geracao;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Leitura enviada pelo inversor solar")
public record GeradoDTO(
        @Schema(description = "Energia gerada no dia até agora (kWh)", example = "12.4") Double gerado,
        @Schema(description = "Potência instantânea (W)", example = "3120") Double potencia) {
}
