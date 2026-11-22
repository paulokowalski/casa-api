package com.kowalski.casaapi.crescimento;

import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.Idade;
import com.kowalski.casaapi.crescimento.crianca.Sexo;
import com.kowalski.casaapi.crescimento.medida.AvaliacaoCrescimento;
import com.kowalski.casaapi.crescimento.medida.Medicao;
import com.kowalski.casaapi.crescimento.medida.MedidaDtos.*;
import com.kowalski.casaapi.crescimento.oms.Indicador;
import com.kowalski.casaapi.crescimento.oms.TabelaOms;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AvaliacaoCrescimentoTest {

    static final TabelaOms OMS = TabelaOmsTest.OMS;
    final AvaliacaoCrescimento avaliacao = new AvaliacaoCrescimento(OMS);
    static final LocalDate NASC = LocalDate.of(2026, 1, 1);

    static Crianca crianca(Double ig) {
        Crianca c = new Crianca();
        c.setId(UUID.randomUUID());
        c.setNome("Teste");
        c.setNascimento(NASC);
        c.setSexo(Sexo.M);
        c.setIdadeGestacionalSemanas(ig);
        c.setPesoNascerKg(3.3);
        return c;
    }

    static Medicao peso(int dias, double kg) {
        Medicao m = new Medicao();
        m.setId(UUID.randomUUID());
        m.setData(NASC.plusDays(dias));
        m.setPesoKg(kg);
        return m;
    }

    double pesoNoZ(int dias, double z) {
        return OMS.valor(Indicador.PESO, Sexo.M, dias, z);
    }

    List<String> codigos(ResumoResponse r) {
        return r.alertas().stream().map(Alerta::codigo).toList();
    }

    @Test
    void crescimentoNaMedianaNaoGeraAlertaDeAtencao() {
        List<Medicao> ms = new ArrayList<>();
        for (int d = 30; d <= 150; d += 30) ms.add(peso(d, pesoNoZ(d, 0)));
        ResumoResponse r = avaliacao.resumo(crianca(39.0), ms, NASC.plusDays(160));
        assertThat(r.alertas()).noneMatch(a -> a.nivel() == Nivel.ATENCAO);
        assertThat(r.ultimas().get(Indicador.PESO).faixa()).isEqualTo(Faixa.ESPERADA);
        assertThat(r.ganhoPeso().situacao()).isEqualTo("ESPERADO");
        assertThat(r.diasDesdeUltimaPesagem()).isEqualTo(10);
    }

    @Test
    void quedaDeDuasLinhasDePercentil() {
        List<Medicao> ms = List.of(peso(60, pesoNoZ(60, 0.2)), peso(120, pesoNoZ(120, -1.5)));
        ResumoResponse r = avaliacao.resumo(crianca(39.0), ms, NASC.plusDays(125));
        assertThat(codigos(r)).contains("CRUZOU_LINHAS");
    }

    @Test
    void foraDaFaixaEPesoQueCaiu() {
        List<Medicao> ms = List.of(peso(60, 5.0), peso(90, 4.6));
        ResumoResponse r = avaliacao.resumo(crianca(39.0), ms, NASC.plusDays(92));
        assertThat(codigos(r)).contains("FORA_DA_FAIXA", "PESO_CAIU");
        assertThat(r.alertas().get(0).nivel()).isEqualTo(Nivel.ATENCAO);
    }

    @Test
    void perdaDePesoNormalDaPrimeiraSemanaNaoAlerta() {
        List<Medicao> ms = List.of(peso(3, 3.15), peso(7, 3.1));
        ResumoResponse r = avaliacao.resumo(crianca(39.0), ms, NASC.plusDays(8));
        assertThat(codigos(r)).doesNotContain("PESO_CAIU");
    }

    @Test
    void semPesagemRecenteSugereRegistrar() {
        ResumoResponse r = avaliacao.resumo(crianca(39.0), List.of(peso(10, 3.6)), NASC.plusDays(80));
        assertThat(codigos(r)).containsExactly("PESAGEM_ATRASADA");
        assertThat(avaliacao.resumo(crianca(39.0), List.of(), NASC.plusDays(5)).alertas())
                .extracting(Alerta::titulo).containsExactly("Nenhuma pesagem registrada");
    }

    @Test
    void prematuroUsaIdadeCorrigida() {
        Crianca c = crianca(32.0);
        assertThat(Idade.correcao(c)).isEqualTo(56);
        assertThat(Idade.diasCurva(c, NASC.plusDays(100))).isEqualTo(44);
        assertThat(Idade.diasCurva(c, NASC.plusDays(800))).isEqualTo(800);
        // antes de completar 40 semanas não há curva da OMS
        assertThat(avaliacao.avaliar(c, Indicador.PESO, NASC.plusDays(10), 1.8).percentil()).isNull();
        // com a idade corrigida, um peso que seria baixo para 100 dias fica na mediana dos 44 dias
        MedidaAvaliada a = avaliacao.avaliar(c, Indicador.PESO, NASC.plusDays(100), pesoNoZ(44, 0));
        assertThat(a.percentil()).isBetween(49.0, 51.0);
    }

    @Test
    void curvaTrazAsLinhasEOPontoDoNascimento() {
        CurvaResponse curva = avaliacao.curva(crianca(39.0), Indicador.PESO, List.of(peso(60, 5.5)), NASC.plusDays(70));
        assertThat(curva.referencia().get(0).dias()).isZero();
        assertThat(curva.referencia().get(curva.referencia().size() - 1).dias()).isGreaterThanOrEqualTo(91);
        assertThat(curva.referencia()).allMatch(p -> p.p3() < p.p15() && p.p15() < p.p50() && p.p50() < p.p85() && p.p85() < p.p97());
        assertThat(curva.pontos()).hasSize(2);
        assertThat(curva.pontos().get(0).nascimento()).isTrue();
        assertThat(curva.idadeCorrigida()).isFalse();
    }

    @Test
    void idadePorExtenso() {
        assertThat(Idade.texto(NASC, NASC)).isEqualTo("0 dias");
        assertThat(Idade.texto(NASC, LocalDate.of(2026, 1, 13))).isEqualTo("12 dias");
        assertThat(Idade.texto(NASC, LocalDate.of(2026, 8, 4))).isEqualTo("7 meses e 3 dias");
        assertThat(Idade.texto(NASC, LocalDate.of(2027, 3, 1))).isEqualTo("1 ano e 2 meses");
        assertThat(Idade.texto(NASC, LocalDate.of(2027, 3, 2))).isEqualTo("1 ano, 2 meses e 1 dia");
    }
}
