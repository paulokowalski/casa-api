package com.kowalski.casaapi.geracao;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Um ponto da série de geração")
public record GeracaoResponse(
        @Schema(description = "Valor (W na série do dia; kWh na série do mês)", example = "3120.0") Double valor,
        @Schema(description = "Momento da leitura") LocalDateTime data) { }
