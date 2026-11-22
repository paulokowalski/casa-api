package com.kowalski.casaapi.crescimento.vacina;

import com.kowalski.casaapi.crescimento.agenda.AgendaService;
import com.kowalski.casaapi.crescimento.agenda.AgendaService.TipoCompromisso;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.vacina.VacinaDtos.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class VacinaServiceTest {

    static final LocalDate NASC = LocalDate.of(2026, 5, 1);

    static Crianca crianca() {
        Crianca c = new Crianca();
        c.setNascimento(NASC);
        return c;
    }

    static VacinaAplicada aplicada(String id, LocalDate data) {
        VacinaAplicada v = new VacinaAplicada();
        v.setVacinaId(id);
        v.setAplicadaEm(data);
        return v;
    }

    static Map<String, DoseResponse> doses(VacinasResponse r) {
        return r.grupos().stream().flatMap(g -> g.doses().stream()).collect(Collectors.toMap(DoseResponse::id, Function.identity()));
    }

    @Test
    void idsUnicos() {
        assertThat(new HashSet<>(CatalogoVacinas.DOSES.stream().map(CatalogoVacinas.Dose::id).toList())).hasSize(CatalogoVacinas.DOSES.size());
    }

    @Test
    void situacoes() {
        LocalDate hoje = LocalDate.of(2026, 11, 16); // 6 meses e 15 dias
        VacinasResponse r = VacinaService.montar(crianca(), List.of(aplicada("bcg", NASC), aplicada("hepb", NASC),
                aplicada("rota1", NASC.plusMonths(2)), aplicada("extra-abc", hoje)), hoje);
        Map<String, DoseResponse> d = doses(r);
        assertThat(d.get("bcg").situacao()).isEqualTo(SituacaoVacina.APLICADA);
        assertThat(d.get("penta1").situacao()).isEqualTo(SituacaoVacina.ATRASADA);
        assertThat(d.get("penta3").situacao()).isEqualTo(SituacaoVacina.TOMAR_AGORA);
        assertThat(d.get("covid2").situacao()).isEqualTo(SituacaoVacina.PROXIMA);
        assertThat(d.get("fa1").situacao()).isEqualTo(SituacaoVacina.FUTURA);
        assertThat(d.get("rota2").situacao()).isEqualTo(SituacaoVacina.ATRASADA);
        assertThat(d.get("rota2").dataLimite()).isEqualTo(NASC.plusDays(239));
        assertThat(doses(VacinaService.montar(crianca(), List.of(), NASC.plusDays(240))).get("rota2").situacao())
                .isEqualTo(SituacaoVacina.PRAZO_ENCERRADO);
        assertThat(r.aplicadas()).isEqualTo(3);
        assertThat(r.proximaData()).isEqualTo(NASC.plusMonths(2));
        assertThat(r.extras()).hasSize(1);
        assertThat(r.grupos().get(0).rotulo()).isEqualTo("Ao nascer");
        assertThat(VacinaService.rotulo(48)).isEqualTo("4 anos");
        assertThat(VacinaService.rotulo(15)).isEqualTo("15 meses");
    }

    @Test
    void agendaAgrupaAtrasadasEMostraMesversario() {
        LocalDate hoje = LocalDate.of(2026, 11, 16);
        Crianca c = crianca();
        var doses = VacinaService.montar(c, List.of(), hoje).grupos().stream().flatMap(g -> g.doses().stream()).toList();
        var ag = AgendaService.montar(c, doses, LocalDate.of(2026, 12, 1), hoje);
        assertThat(ag.get(0).tipo()).isEqualTo(TipoCompromisso.VACINA);
        assertThat(ag.get(0).atrasado()).isTrue();
        assertThat(ag.get(0).data()).isEqualTo(NASC);
        assertThat(ag).anyMatch(x -> x.tipo() == TipoCompromisso.CONSULTA && x.dias() == 15);
        assertThat(ag).anyMatch(x -> x.tipo() == TipoCompromisso.MESVERSARIO && x.referencias().equals(List.of("mes-7")));
        assertThat(ag).noneMatch(x -> x.referencias().contains("fa1")); // 9 meses: fora dos 60 dias
    }
}
