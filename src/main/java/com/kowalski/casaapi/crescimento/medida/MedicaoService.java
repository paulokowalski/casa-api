package com.kowalski.casaapi.crescimento.medida;

import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaService;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.medida.MedidaDtos.*;
import com.kowalski.casaapi.crescimento.diario.RegistroRepository;
import com.kowalski.casaapi.crescimento.oms.Indicador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicaoService {

    private final MedicaoRepository medicoes;
    private final CriancaService criancas;
    private final AcessoFamilia acesso;
    private final AvaliacaoCrescimento avaliacao;
    private final Clock relogio;
    private final RegistroRepository registros;

    /**
     * Grava a medição que acompanha um registro do diário, ou apaga se o registro ficou sem medidas.
     * O acesso já foi conferido por quem chama. Devolve null quando não há medidas.
     */
    @Transactional
    public Medicao gravarDoRegistro(Crianca c, UUID medicaoId, LocalDate data, Double pesoKg, Double comprimentoCm, Double perimetroCm) {
        Medicao m = medicaoId == null ? null : medicoes.findById(medicaoId).filter(x -> x.getCriancaId().equals(c.getId())).orElse(null);
        if (pesoKg == null && comprimentoCm == null && perimetroCm == null) {
            if (m != null) medicoes.delete(m);
            return null;
        }
        if (m == null) {
            m = new Medicao();
            m.setCriancaId(c.getId());
            m.setCriadoPor(acesso.usuarioId());
        } else {
            m.setAtualizadoEm(Instant.now());
        }
        aplicar(c, m, new MedicaoInput(data, pesoKg, comprimentoCm, perimetroCm, m.getObservacao()));
        return medicoes.save(m);
    }

    @Transactional
    public void excluirDoRegistro(UUID medicaoId) {
        if (medicaoId != null) medicoes.findById(medicaoId).ifPresent(medicoes::delete);
    }

    public MedicaoResponse resposta(Crianca c, Medicao m) {
        return avaliacao.resposta(c, m);
    }

    /** Mais recentes primeiro. */
    @Transactional(readOnly = true)
    public List<MedicaoResponse> listar(UUID criancaId) {
        Crianca c = criancas.carregar(criancaId, false);
        return medicoes.findByCriancaIdOrderByDataAscCriadoEmAsc(criancaId).stream()
                .sorted(Comparator.comparing(Medicao::getData).thenComparing(Medicao::getCriadoEm).reversed())
                .map(m -> avaliacao.resposta(c, m)).toList();
    }

    @Transactional
    public MedicaoResponse criar(UUID criancaId, MedicaoInput dados) {
        Crianca c = criancas.carregar(criancaId, true);
        Medicao m = new Medicao();
        m.setCriancaId(criancaId);
        m.setCriadoPor(acesso.usuarioId());
        aplicar(c, m, dados);
        return avaliacao.resposta(c, medicoes.save(m));
    }

    @Transactional
    public MedicaoResponse atualizar(UUID criancaId, UUID id, MedicaoInput dados) {
        Crianca c = criancas.carregar(criancaId, true);
        Medicao m = daCrianca(criancaId, id);
        aplicar(c, m, dados);
        m.setAtualizadoEm(Instant.now());
        return avaliacao.resposta(c, medicoes.save(m));
    }

    @Transactional
    public void excluir(UUID criancaId, UUID id) {
        criancas.carregar(criancaId, true);
        medicoes.delete(daCrianca(criancaId, id));
    }

    @Transactional(readOnly = true)
    public ResumoResponse resumo(UUID criancaId) {
        Crianca c = criancas.carregar(criancaId, false);
        LocalDate hoje = LocalDate.now(relogio);
        return avaliacao.resumo(c, medicoes.findByCriancaIdOrderByDataAscCriadoEmAsc(criancaId), hoje,
                registros.proximaConsulta(criancaId, hoje));
    }

    @Transactional(readOnly = true)
    public CurvaResponse curva(UUID criancaId, Indicador indicador) {
        Crianca c = criancas.carregar(criancaId, false);
        return avaliacao.curva(c, indicador, medicoes.findByCriancaIdOrderByDataAscCriadoEmAsc(criancaId), LocalDate.now(relogio));
    }

    private Medicao daCrianca(UUID criancaId, UUID id) {
        return medicoes.findById(id).filter(m -> m.getCriancaId().equals(criancaId))
                .orElseThrow(() -> new NaoEncontradoException("Medição não encontrada"));
    }

    private void aplicar(Crianca c, Medicao m, MedicaoInput d) {
        if (d == null || d.data() == null) throw new IllegalArgumentException("Informe a data");
        if (d.data().isBefore(c.getNascimento())) throw new IllegalArgumentException("A data é anterior ao nascimento");
        if (d.data().isAfter(LocalDate.now(relogio))) throw new IllegalArgumentException("A data não pode ser no futuro");
        if (d.pesoKg() == null && d.comprimentoCm() == null && d.perimetroCm() == null)
            throw new IllegalArgumentException("Informe pelo menos uma medida");
        faixa(d.pesoKg(), 0.3, 80, "O peso deve estar em kg, por exemplo 7,35");
        faixa(d.comprimentoCm(), 20, 200, "O comprimento deve estar em cm, por exemplo 66,5");
        faixa(d.perimetroCm(), 18, 65, "O perímetro cefálico deve estar em cm, por exemplo 43,0");
        String obs = d.observacao() == null ? null : d.observacao().trim();
        if (obs != null && obs.length() > 1000) throw new IllegalArgumentException("A observação pode ter até 1000 caracteres");
        m.setData(d.data());
        m.setPesoKg(d.pesoKg());
        m.setComprimentoCm(d.comprimentoCm());
        m.setPerimetroCm(d.perimetroCm());
        m.setObservacao(obs == null || obs.isEmpty() ? null : obs);
    }

    private static void faixa(Double v, double min, double max, String mensagem) {
        if (v != null && (v.isNaN() || v < min || v > max)) throw new IllegalArgumentException(mensagem);
    }
}
