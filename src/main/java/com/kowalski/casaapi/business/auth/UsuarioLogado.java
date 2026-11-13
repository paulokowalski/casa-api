package com.kowalski.casaapi.business.auth;

import com.kowalski.casaapi.business.enumerator.Papel;
import com.kowalski.casaapi.config.security.UsuarioAutenticado;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioLogado {

    public UsuarioAutenticado atual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsuarioAutenticado usuario) return usuario;
        throw new AccessDeniedException("Faça login para continuar");
    }

    public boolean isAdmin() {
        return atual().getPapel() == Papel.ADMIN;
    }

    public Long pessoaId(Long solicitado) {
        UsuarioAutenticado u = atual();
        if (solicitado == null) return u.getPessoaId();
        if (u.getPapel() == Papel.ADMIN || solicitado.equals(u.getPessoaId())) return solicitado;
        throw new AccessDeniedException("Você só pode ver os seus próprios dados");
    }

    public String nomePessoa(String solicitado) {
        UsuarioAutenticado u = atual();
        if (solicitado == null || solicitado.isBlank()) return u.getNome();
        if (u.getPapel() == Papel.ADMIN || mesmoNome(solicitado, u.getNome())) return solicitado;
        throw new AccessDeniedException("Você só pode ver os seus próprios dados");
    }

    public boolean podeAcessar(Long donoId) {
        UsuarioAutenticado u = atual();
        return u.getPapel() == Papel.ADMIN || (donoId != null && donoId.equals(u.getPessoaId()));
    }

    public boolean podeAcessarNome(String nomeDono) {
        UsuarioAutenticado u = atual();
        return u.getPapel() == Papel.ADMIN || mesmoNome(nomeDono, u.getNome());
    }

    static boolean mesmoNome(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }
}
