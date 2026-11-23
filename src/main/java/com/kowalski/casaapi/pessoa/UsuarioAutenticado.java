package com.kowalski.casaapi.pessoa;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class UsuarioAutenticado extends AbstractAuthenticationToken {

    private final Long pessoaId;
    private final String nome;
    private final Papel papel;

    public UsuarioAutenticado(Long pessoaId, String nome, Papel papel) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + papel.name())));
        this.pessoaId = pessoaId;
        this.nome = nome;
        this.papel = papel;
        setAuthenticated(true);
    }

    public Long getPessoaId() {
        return pessoaId;
    }

    public String getNome() {
        return nome;
    }

    public Papel getPapel() {
        return papel;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return pessoaId;
    }

    @Override
    public String getName() {
        return String.valueOf(pessoaId);
    }
}
