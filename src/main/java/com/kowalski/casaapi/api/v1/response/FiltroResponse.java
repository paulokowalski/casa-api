package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Opção de filtro")
public record FiltroResponse(
        @Schema(description = "Valor a enviar nos outros endpoints", example = "11") String codigo,
        @Schema(description = "Texto para mostrar", example = "NOVEMBRO") String descricao) { }