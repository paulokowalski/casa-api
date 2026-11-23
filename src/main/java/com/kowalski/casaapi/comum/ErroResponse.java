package com.kowalski.casaapi.comum;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corpo de toda resposta de erro")
public record ErroResponse(
        @Schema(description = "Mensagem para mostrar ao usuário", example = "O parâmetro pessoa é obrigatório") String mensagem) {
}
