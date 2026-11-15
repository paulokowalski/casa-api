package com.kowalski.casaapi.api.v1.input;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credenciais de acesso")
public record LoginInput(
        @Schema(description = "E-mail da pessoa", example = "paulo@exemplo.com") String email,
        @Schema(description = "Senha", example = "minha-senha") String senha) {
}
