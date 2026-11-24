package com.kowalski.casaapi.config.crescimento;

import com.kowalski.casaapi.pessoa.Pessoa;
import com.kowalski.casaapi.pessoa.PessoaService;
import com.kowalski.casaapi.pessoa.UsuarioLogado;
import com.kowalski.casaapi.crescimento.identidade.DiretorioUsuarios;
import com.kowalski.casaapi.crescimento.identidade.UsuarioAtual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IdentidadeCrescimento implements UsuarioAtual, DiretorioUsuarios {

    private final UsuarioLogado usuarioLogado;
    private final PessoaService pessoas;

    @Override
    public long id() {
        return usuarioLogado.atual().getPessoaId();
    }

    @Override
    public Map<Long, String> nomes(Collection<Long> ids) {
        Map<Long, String> r = new HashMap<>();
        pessoas.porIds(ids).forEach(p -> r.put(p.getId(), nomeBonito(p.getNome())));
        return r;
    }

    @Override
    public boolean existe(long id) {
        return pessoas.porId(id).map(Pessoa::isAtivo).orElse(false);
    }

    static String nomeBonito(String nome) {
        if (nome == null || nome.isBlank()) return nome;
        StringBuilder sb = new StringBuilder();
        for (String parte : nome.trim().toLowerCase().split("\\s+")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(parte.charAt(0))).append(parte.substring(1));
        }
        return sb.toString();
    }
}
