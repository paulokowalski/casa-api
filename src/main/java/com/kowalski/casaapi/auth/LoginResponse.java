package com.kowalski.casaapi.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Resultado do login")
public record LoginResponse(
        @Schema(description = "Token JWT; envie como Authorization: Bearer <token>") String token,
        @Schema(description = "Quando o token expira") Instant expiraEm,
        @Schema(description = "Usuário logado") UsuarioResponse usuario) {
}
