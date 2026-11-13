package com.kowalski.casaapi.support;

import com.kowalski.casaapi.business.enumerator.Papel;
import com.kowalski.casaapi.business.model.Pessoa;
import com.kowalski.casaapi.config.security.UsuarioAutenticado;
import org.springframework.security.core.context.SecurityContextHolder;

public final class Login {

    public static final long ADMIN_ID = 3L;
    public static final long ANA_ID = 1L;
    public static final long BRUNO_ID = 2L;

    private Login() {
    }

    public static void comoAdmin() {
        como(ADMIN_ID, "ADMIN", Papel.ADMIN);
    }

    public static void comoAna() {
        como(ANA_ID, "ANA", Papel.USUARIO);
    }

    public static void como(long id, String nome, Papel papel) {
        SecurityContextHolder.getContext().setAuthentication(new UsuarioAutenticado(id, nome, papel));
    }

    public static void sair() {
        SecurityContextHolder.clearContext();
    }

    public static Pessoa pessoa(long id, String nome) {
        Pessoa p = new Pessoa();
        p.setId(id);
        p.setNome(nome);
        p.setEmail(nome.toLowerCase() + "@exemplo.com");
        return p;
    }
}
