package com.kowalski.casaapi.comum;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mensagem de confirmação")
public record MensagemResponse(
        @Schema(description = "Mensagem", example = "Cadastro realizado com sucesso.") String mensagem) {
}
