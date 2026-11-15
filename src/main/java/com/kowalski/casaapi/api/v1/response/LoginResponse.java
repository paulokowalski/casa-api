package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Resultado do login")
public record LoginResponse(
        @Schema(description = "Token JWT; envie como Authorization: Bearer <token>") String token,
        @Schema(description = "Quando o token expira") Instant expiraEm,
        @Schema(description = "Usuário logado") UsuarioResponse usuario) {
}
