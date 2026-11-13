package com.kowalski.casaapi.api.v1.response;

import java.time.Instant;

public record LoginResponse(String token, Instant expiraEm, UsuarioResponse usuario) {
}
