package com.kowalski.casaapi.crescimento.marco;

import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.marco.MarcoDtos.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarcoServiceTest {

    static Crianca crianca(LocalDate nasc, Double semanas) {
        Crianca c = new Crianca();
        c.setNascimento(nasc);
        if (semanas != null) c.setIdadeGestacionalSemanas(semanas);
        return c;
    }

    @Test
    void catalogoTemIdsUnicosEQuatroMarcosPorFaixa() {
        assertThat(new HashSet<>(CatalogoMarcos.MARCOS.stream().map(CatalogoMarcos.Marco::id).toList())).hasSize(CatalogoMarcos.MARCOS.size());
        for (var f : CatalogoMarcos.FAIXAS) assertThat(CatalogoMarcos.MARCOS.stream().filter(m -> m.faixaId().equals(f.id()))).hasSize(4);
    }

    @Test
    void faseAtualEAtencao() {
        LocalDate hoje = LocalDate.of(2026, 11, 16);
        Crianca c = crianca(hoje.minusMonths(7), 39.0); // 7 meses: faixa 6-9
        MarcoAtingido m = new MarcoAtingido();
        m.setMarcoId("m03d");
        m.setAtingidoEm(hoje.minusMonths(2));
        MarcosResponse r = MarcoService.montar(c, List.of(m), hoje);
        assertThat(r.faixaAtualId()).isEqualTo("f4");
        assertThat(r.idadeCorrigida()).isFalse();
        assertThat(r.marcosAtencao()).isEqualTo(4 * 4 - 1);
        FaixaResponse f3 = r.faixas().get(3);
        assertThat(f3.atingidos()).isEqualTo(1);
        assertThat(f3.marcos().stream().filter(x -> x.id().equals("m03d")).findFirst().orElseThrow().situacao()).isEqualTo(SituacaoMarco.ATINGIDO);
        assertThat(r.faixas().get(4).marcos()).allMatch(x -> x.situacao() == SituacaoMarco.ESPERADO);
        assertThat(r.faixas().get(5).situacao()).isEqualTo(SituacaoFaixa.PROXIMA);
    }

    @Test
    void prematuroUsaIdadeCorrigida() {
        LocalDate hoje = LocalDate.of(2026, 11, 16);
        // 4 meses de idade real (123 dias), nasceu com 31 semanas: 63 dias a menos -> 60 dias, fase de 1 a 2 meses
        MarcosResponse r = MarcoService.montar(crianca(hoje.minusMonths(4), 31.0), List.of(), hoje);
        assertThat(r.idadeCorrigida()).isTrue();
        assertThat(r.idadeDias()).isEqualTo(60);
        assertThat(r.faixaAtualId()).isEqualTo("f1");
        assertThat(MarcoService.montar(crianca(hoje.minusMonths(4), 39.0), List.of(), hoje).faixaAtualId()).isEqualTo("f3");
    }
}
