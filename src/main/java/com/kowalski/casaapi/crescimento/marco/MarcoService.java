package com.kowalski.casaapi.crescimento.marco;

import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaService;
import com.kowalski.casaapi.crescimento.crianca.Idade;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.marco.CatalogoMarcos.Faixa;
import com.kowalski.casaapi.crescimento.marco.CatalogoMarcos.Marco;
import com.kowalski.casaapi.crescimento.marco.MarcoDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarcoService {

    static final double DIAS_MES = 30.4375;
    static final String AVISO = "Cada criança tem seu ritmo. Os marcos indicam o que costuma acontecer em cada fase; se algo de uma fase que "
            + "já passou ainda não aconteceu, converse com o pediatra na próxima consulta.";

    private final MarcoAtingidoRepository repositorio;
    private final CriancaService criancas;
    private final AcessoFamilia acesso;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public MarcosResponse marcos(UUID criancaId) {
        Crianca c = criancas.carregar(criancaId, false);
        return montar(c, repositorio.findByCriancaId(criancaId), LocalDate.now(relogio));
    }

    @Transactional
    public MarcoResponse marcar(UUID criancaId, String marcoId, MarcoInput dados) {
        Crianca c = criancas.carregar(criancaId, true);
        Marco marco = CatalogoMarcos.marco(marcoId).orElseThrow(() -> new NaoEncontradoException("Marco desconhecido: " + marcoId));
        LocalDate hoje = LocalDate.now(relogio);
        LocalDate data = dados == null || dados.atingidoEm() == null ? hoje : dados.atingidoEm();
        if (data.isBefore(c.getNascimento())) throw new IllegalArgumentException("A data é anterior ao nascimento");
        if (data.isAfter(hoje)) throw new IllegalArgumentException("A data não pode ser no futuro");
        String nota = dados == null || dados.nota() == null || dados.nota().isBlank() ? null : dados.nota().trim();
        if (nota != null && nota.length() > 500) throw new IllegalArgumentException("A anotação pode ter até 500 caracteres");
        MarcoAtingido m = repositorio.findByCriancaIdAndMarcoId(criancaId, marcoId).orElseGet(() -> {
            MarcoAtingido novo = new MarcoAtingido();
            novo.setCriancaId(criancaId);
            novo.setMarcoId(marcoId);
            novo.setMarcadoPor(acesso.usuarioId());
            return novo;
        });
        m.setAtingidoEm(data);
        m.setNota(nota);
        m.setAtualizadoEm(Instant.now());
        repositorio.save(m);
        return new MarcoResponse(marco.id(), marco.area(), marco.texto(), SituacaoMarco.ATINGIDO, data,
                Idade.texto(c.getNascimento(), data), nota);
    }

    @Transactional
    public void desmarcar(UUID criancaId, String marcoId) {
        criancas.carregar(criancaId, true);
        repositorio.findByCriancaIdAndMarcoId(criancaId, marcoId).ifPresent(repositorio::delete);
    }

    static int idadeDias(Crianca c, LocalDate hoje) {
        return Idade.diasCurva(c, hoje);
    }

    static MarcosResponse montar(Crianca c, List<MarcoAtingido> registrados, LocalDate hoje) {
        Map<String, MarcoAtingido> porId = registrados.stream().collect(Collectors.toMap(MarcoAtingido::getMarcoId, m -> m, (a, b) -> a));
        int dias = idadeDias(c, hoje);
        Faixa atual = CatalogoMarcos.faixaDaIdade(Math.max(0, dias) / DIAS_MES);
        int iAtual = CatalogoMarcos.FAIXAS.indexOf(atual);
        int atencao = 0;
        List<FaixaResponse> faixas = new ArrayList<>();
        for (int i = 0; i < CatalogoMarcos.FAIXAS.size(); i++) {
            Faixa f = CatalogoMarcos.FAIXAS.get(i);
            SituacaoFaixa sf = i < iAtual ? SituacaoFaixa.PASSADA : i == iAtual ? SituacaoFaixa.ATUAL
                    : i == iAtual + 1 ? SituacaoFaixa.PROXIMA : SituacaoFaixa.FUTURA;
            List<MarcoResponse> ms = new ArrayList<>();
            int feitos = 0;
            for (Marco m : CatalogoMarcos.MARCOS) {
                if (!m.faixaId().equals(f.id())) continue;
                MarcoAtingido r = porId.get(m.id());
                SituacaoMarco sm = r != null ? SituacaoMarco.ATINGIDO : sf == SituacaoFaixa.PASSADA ? SituacaoMarco.ATENCAO
                        : sf == SituacaoFaixa.ATUAL ? SituacaoMarco.ESPERADO : SituacaoMarco.FUTURO;
                if (r != null) feitos++;
                if (sm == SituacaoMarco.ATENCAO) atencao++;
                ms.add(new MarcoResponse(m.id(), m.area(), m.texto(), sm, r == null ? null : r.getAtingidoEm(),
                        r == null ? null : Idade.texto(c.getNascimento(), r.getAtingidoEm()), r == null ? null : r.getNota()));
            }
            faixas.add(new FaixaResponse(f.id(), f.nome(), f.inicioMeses(), f.fimMeses(), sf, f.brincadeiras(), feitos, ms.size(), ms));
        }
        return new MarcosResponse(CatalogoMarcos.VERSAO, CatalogoMarcos.FONTE, dias, Idade.diasCorrigidos(c, hoje) != null,
                atual.id(), atencao, AVISO, faixas);
    }
}
