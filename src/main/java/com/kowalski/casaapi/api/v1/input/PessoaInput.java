package com.kowalski.casaapi.api.v1.input;

import com.kowalski.casaapi.business.enumerator.Papel;

public record PessoaInput(String nome, String email, String senha, Papel papel, Boolean ativo) {
}
