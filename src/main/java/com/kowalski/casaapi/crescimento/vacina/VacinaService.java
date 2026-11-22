package com.kowalski.casaapi.crescimento.vacina;

import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaService;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.vacina.CatalogoVacinas.Dose;
import com.kowalski.casaapi.crescimento.vacina.VacinaDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VacinaService {

    static final int DIAS_TOLERANCIA = 30;
    static final int DIAS_PROXIMA = 30;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final VacinaAplicadaRepository repositorio;
    private final CriancaService criancas;
    private final AcessoFamilia acesso;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public VacinasResponse vacinas(UUID criancaId) {
        Crianca c = criancas.carregar(criancaId, false);
        return montar(c, repositorio.findByCriancaId(criancaId), LocalDate.now(relogio));
    }

    @Transactional
    public DoseResponse aplicar(UUID criancaId, String vacinaId, VacinaInput dados) {
        Crianca c = criancas.carregar(criancaId, true);
        Dose dose = CatalogoVacinas.dose(vacinaId).orElseThrow(() -> new NaoEncontradoException("Vacina desconhecida: " + vacinaId));
        VacinaAplicada v = gravar(c, vacinaId, null, dados == null ? null : dados.aplicadaEm(), dados == null ? null : dados.lote(),
                dados == null ? null : dados.local());
        return dose(c, dose, v, LocalDate.now(relogio));
    }

    @Transactional
    public ExtraResponse aplicarExtra(UUID criancaId, VacinaExtraInput dados) {
        Crianca c = criancas.carregar(criancaId, true);
        if (dados == null) throw new IllegalArgumentException("Informe a vacina");
        String nome = limpo(dados.nome(), 80, "O nome da vacina pode ter até 80 caracteres");
        if (nome == null) throw new IllegalArgumentException("Informe o nome da vacina");
        byte[] b = new byte[5];
        ALEATORIO.nextBytes(b);
        VacinaAplicada v = gravar(c, "extra-" + HexFormat.of().formatHex(b), nome, dados.aplicadaEm(), dados.lote(), dados.local());
        return extra(v);
    }

    @Transactional
    public void remover(UUID criancaId, String vacinaId) {
        criancas.carregar(criancaId, true);
        VacinaAplicada v = repositorio.findByCriancaIdAndVacinaId(criancaId, vacinaId)
                .orElseThrow(() -> new NaoEncontradoException("Esta vacina não está registrada"));
        repositorio.delete(v);
    }

    private VacinaAplicada gravar(Crianca c, String vacinaId, String nome, LocalDate data, String lote, String local) {
        LocalDate hoje = LocalDate.now(relogio);
        if (data == null) data = hoje;
        if (data.isBefore(c.getNascimento())) throw new IllegalArgumentException("A data é anterior ao nascimento");
        if (data.isAfter(hoje)) throw new IllegalArgumentException("A data não pode ser no futuro");
        VacinaAplicada v = repositorio.findByCriancaIdAndVacinaId(c.getId(), vacinaId).orElseGet(() -> {
            VacinaAplicada n = new VacinaAplicada();
            n.setCriancaId(c.getId());
            n.setVacinaId(vacinaId);
            n.setRegistradoPor(acesso.usuarioId());
            return n;
        });
        v.setNome(nome);
        v.setAplicadaEm(data);
        v.setLote(limpo(lote, 40, "O lote pode ter até 40 caracteres"));
        v.setLocal(limpo(local, 80, "O local pode ter até 80 caracteres"));
        v.setAtualizadoEm(Instant.now());
        return repositorio.save(v);
    }

    public static VacinasResponse montar(Crianca c, List<VacinaAplicada> aplicadas, LocalDate hoje) {
        Map<String, VacinaAplicada> porId = aplicadas.stream().collect(Collectors.toMap(VacinaAplicada::getVacinaId, v -> v, (a, b) -> a));
        Map<Integer, List<DoseResponse>> porIdade = new TreeMap<>();
        for (Dose d : CatalogoVacinas.DOSES) porIdade.computeIfAbsent(d.meses(), k -> new ArrayList<>()).add(dose(c, d, porId.get(d.id()), hoje));
        List<GrupoResponse> grupos = new ArrayList<>();
        int feitas = 0, pendentes = 0;
        LocalDate proxima = null;
        for (var e : porIdade.entrySet()) {
            int ok = 0;
            for (DoseResponse d : e.getValue()) {
                if (d.situacao() == SituacaoVacina.APLICADA) ok++;
                else if (d.situacao() == SituacaoVacina.TOMAR_AGORA || d.situacao() == SituacaoVacina.ATRASADA) pendentes++;
                if (d.situacao() != SituacaoVacina.APLICADA && d.situacao() != SituacaoVacina.PRAZO_ENCERRADO
                        && (proxima == null || d.dataPrevista().isBefore(proxima))) proxima = d.dataPrevista();
            }
            feitas += ok;
            grupos.add(new GrupoResponse(e.getKey(), rotulo(e.getKey()), c.getNascimento().plusMonths(e.getKey()), ok, e.getValue().size(), e.getValue()));
        }
        List<ExtraResponse> extras = aplicadas.stream().filter(v -> v.getVacinaId().startsWith("extra-"))
                .sorted(Comparator.comparing(VacinaAplicada::getAplicadaEm)).map(VacinaService::extra).toList();
        return new VacinasResponse(CatalogoVacinas.VERSAO, CatalogoVacinas.AVISO, feitas, CatalogoVacinas.DOSES.size(), pendentes,
                proxima, grupos, extras);
    }

    static DoseResponse dose(Crianca c, Dose d, VacinaAplicada v, LocalDate hoje) {
        LocalDate prevista = c.getNascimento().plusMonths(d.meses());
        LocalDate limite = d.prazoMaximoDias() == null ? null : c.getNascimento().plusDays(d.prazoMaximoDias());
        SituacaoVacina s;
        if (v != null) s = SituacaoVacina.APLICADA;
        else if (limite != null && hoje.isAfter(limite)) s = SituacaoVacina.PRAZO_ENCERRADO;
        else if (!prevista.isAfter(hoje)) s = prevista.plusDays(DIAS_TOLERANCIA).isBefore(hoje) ? SituacaoVacina.ATRASADA : SituacaoVacina.TOMAR_AGORA;
        else s = prevista.isAfter(hoje.plusDays(DIAS_PROXIMA)) ? SituacaoVacina.FUTURA : SituacaoVacina.PROXIMA;
        return new DoseResponse(d.id(), d.vacina(), d.dose(), d.meses(), prevista, limite, s,
                v == null ? null : v.getAplicadaEm(), v == null ? null : v.getLote(), v == null ? null : v.getLocal());
    }

    static String rotulo(int meses) {
        if (meses == 0) return "Ao nascer";
        if (meses < 12) return meses + (meses == 1 ? " mês" : " meses");
        if (meses % 12 == 0) return meses / 12 + (meses == 12 ? " ano" : " anos");
        return meses + " meses";
    }

    private static ExtraResponse extra(VacinaAplicada v) {
        return new ExtraResponse(v.getVacinaId(), v.getNome(), v.getAplicadaEm(), v.getLote(), v.getLocal());
    }

    private static String limpo(String s, int max, String mensagem) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        if (s.length() > max) throw new IllegalArgumentException(mensagem);
        return s;
    }
}
