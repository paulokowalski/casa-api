package com.kowalski.casaapi.crescimento.familia;

import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.familia.FamiliaDtos.*;
import com.kowalski.casaapi.crescimento.identidade.DiretorioUsuarios;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FamiliaService {

    private final FamiliaRepository familias;
    private final FamiliaMembroRepository membros;
    private final AcessoFamilia acesso;
    private final DiretorioUsuarios usuarios;

    @Transactional(readOnly = true)
    public List<FamiliaResponse> minhas() {
        return familias.doUsuario(acesso.usuarioId()).stream().map(this::resposta).toList();
    }

    @Transactional
    public FamiliaResponse criar(NovaFamilia dados) {
        String nome = dados == null || dados.nome() == null ? "" : dados.nome().trim();
        if (nome.isEmpty()) throw new IllegalArgumentException("Informe o nome da família");
        if (nome.length() > 80) throw new IllegalArgumentException("O nome da família pode ter até 80 caracteres");
        Familia f = new Familia();
        f.setNome(nome);
        f = familias.save(f);
        FamiliaMembro m = new FamiliaMembro();
        m.setFamiliaId(f.getId());
        m.setUsuarioId(acesso.usuarioId());
        m.setPapel(PapelFamilia.RESPONSAVEL);
        membros.save(m);
        return resposta(f);
    }

    @Transactional
    public FamiliaResponse salvarMembro(UUID familiaId, NovoMembro dados) {
        acesso.exigirEscrita(familiaId);
        if (dados == null || dados.usuarioId() == null || dados.papel() == null)
            throw new IllegalArgumentException("Informe o usuário e o papel");
        if (!usuarios.existe(dados.usuarioId())) throw new IllegalArgumentException("Usuário não encontrado");
        FamiliaMembro m = membros.findByFamiliaIdAndUsuarioId(familiaId, dados.usuarioId()).orElseGet(() -> {
            FamiliaMembro novo = new FamiliaMembro();
            novo.setFamiliaId(familiaId);
            novo.setUsuarioId(dados.usuarioId());
            return novo;
        });
        if (m.getPapel() == PapelFamilia.RESPONSAVEL && dados.papel() != PapelFamilia.RESPONSAVEL) garantirOutroResponsavel(familiaId);
        m.setPapel(dados.papel());
        membros.save(m);
        return resposta(familias.findById(familiaId).orElseThrow());
    }

    @Transactional
    public void removerMembro(UUID familiaId, long usuarioId) {
        boolean saindo = usuarioId == acesso.usuarioId();
        if (saindo) acesso.exigirLeitura(familiaId);
        else acesso.exigirEscrita(familiaId);
        FamiliaMembro m = membros.findByFamiliaIdAndUsuarioId(familiaId, usuarioId)
                .orElseThrow(() -> new NaoEncontradoException("Membro não encontrado"));
        if (m.getPapel() == PapelFamilia.RESPONSAVEL) garantirOutroResponsavel(familiaId);
        membros.delete(m);
    }

    private void garantirOutroResponsavel(UUID familiaId) {
        if (membros.countByFamiliaIdAndPapel(familiaId, PapelFamilia.RESPONSAVEL) <= 1)
            throw new IllegalArgumentException("A família precisa ter pelo menos um responsável");
    }

    private FamiliaResponse resposta(Familia f) {
        List<FamiliaMembro> lista = membros.findByFamiliaIdOrderByCriadoEm(f.getId());
        Map<Long, String> nomes = usuarios.nomes(lista.stream().map(FamiliaMembro::getUsuarioId).toList());
        PapelFamilia meu = lista.stream().filter(m -> m.getUsuarioId() == acesso.usuarioId())
                .map(FamiliaMembro::getPapel).findFirst().orElse(null);
        return new FamiliaResponse(f.getId(), f.getNome(), meu, lista.stream()
                .map(m -> new MembroResponse(m.getUsuarioId(), nomes.get(m.getUsuarioId()), m.getPapel(), m.getCriadoEm()))
                .toList());
    }
}
