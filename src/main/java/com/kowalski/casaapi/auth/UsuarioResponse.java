package com.kowalski.casaapi.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import com.kowalski.casaapi.pessoa.Papel;
import com.kowalski.casaapi.pessoa.Pessoa;

@Schema(description = "Usuário")
public record UsuarioResponse(
        @Schema(description = "Id da pessoa", example = "1") Long id,
        @Schema(description = "Nome", example = "PAULO") String nome,
        @Schema(description = "E-mail", example = "paulo@exemplo.com") String email,
        @Schema(description = "Papel", example = "ADMIN") Papel papel) {

    public static UsuarioResponse de(Pessoa p) {
        return new UsuarioResponse(p.getId(), p.getNome(), p.getEmail(), p.getPapel());
    }
}
