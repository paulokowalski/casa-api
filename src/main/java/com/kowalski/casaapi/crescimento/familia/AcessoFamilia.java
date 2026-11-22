package com.kowalski.casaapi.crescimento.familia;

import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.comum.SemPermissaoException;
import com.kowalski.casaapi.crescimento.identidade.UsuarioAtual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Regra de acesso de todo o módulo: só membros veem os dados da família e só responsáveis alteram.
 * Quem não é membro recebe "não encontrado", para não revelar que a família ou a criança existem.
 */
@Component
@RequiredArgsConstructor
public class AcessoFamilia {

    private final FamiliaMembroRepository membros;
    private final UsuarioAtual usuario;

    public PapelFamilia papel(UUID familiaId) {
        return membros.findByFamiliaIdAndUsuarioId(familiaId, usuario.id())
                .map(FamiliaMembro::getPapel)
                .orElseThrow(() -> new NaoEncontradoException("Família não encontrada"));
    }

    public void exigirLeitura(UUID familiaId) {
        papel(familiaId);
    }

    public void exigirEscrita(UUID familiaId) {
        if (papel(familiaId) != PapelFamilia.RESPONSAVEL)
            throw new SemPermissaoException("Só os responsáveis pela criança podem alterar estes dados");
    }

    public boolean podeEscrever(UUID familiaId) {
        return membros.findByFamiliaIdAndUsuarioId(familiaId, usuario.id())
                .map(m -> m.getPapel() == PapelFamilia.RESPONSAVEL).orElse(false);
    }

    public long usuarioId() {
        return usuario.id();
    }
}
