package com.kowalski.casaapi.api.v1.response;

import com.kowalski.casaapi.business.enumerator.Papel;
import com.kowalski.casaapi.business.model.Pessoa;

public record UsuarioResponse(Long id, String nome, String email, Papel papel) {

    public static UsuarioResponse de(Pessoa p) {
        return new UsuarioResponse(p.getId(), p.getNome(), p.getEmail(), p.getPapel());
    }
}
