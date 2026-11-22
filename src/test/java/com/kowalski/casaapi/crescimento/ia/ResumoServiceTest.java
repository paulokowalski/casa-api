package com.kowalski.casaapi.crescimento.ia;

import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.Sexo;
import com.kowalski.casaapi.crescimento.diario.Registro;
import com.kowalski.casaapi.crescimento.diario.TipoRegistro;
import com.kowalski.casaapi.crescimento.marco.MarcoAtingido;
import com.kowalski.casaapi.crescimento.medida.MedidaDtos.Faixa;
import com.kowalski.casaapi.crescimento.medida.MedidaDtos.MedicaoResponse;
import com.kowalski.casaapi.crescimento.medida.MedidaDtos.MedidaAvaliada;
import com.kowalski.casaapi.crescimento.vacina.VacinaAplicada;
import com.kowalski.casaapi.crescimento.vacina.VacinaService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResumoServiceTest {

    static final LocalDate NASC = LocalDate.of(2026, 7, 31);

    static Crianca crianca() {
        Crianca c = new Crianca();
        c.setNome("Ana Clara Souza");
        c.setNascimento(NASC);
        c.setSexo(Sexo.F);
        return c;
    }

    static MedicaoResponse peso(LocalDate data, double kg, double percentil) {
        return new MedicaoResponse(UUID.randomUUID(), data, 0, "x", new MedidaAvaliada(kg, 0.0, percentil, Faixa.ESPERADA),
                null, null, null, null, null);
    }

    static Registro registro(LocalDate data, String texto) {
        Registro r = new Registro();
        r.setData(data);
        r.setTipo(TipoRegistro.MOMENTO);
        r.setTexto(texto);
        r.setProfissional("Dra. Fulana");
        return r;
    }

    @Test
    void limitesDoMesSeguemOsMesversarios() {
        Crianca c = crianca();
        assertThat(ResumoService.inicio(c, 0)).isEqualTo(NASC);
        assertThat(ResumoService.fim(c, 0)).isEqualTo(LocalDate.of(2026, 8, 30));
        assertThat(ResumoService.inicio(c, 1)).isEqualTo(LocalDate.of(2026, 8, 31));
        assertThat(ResumoService.fim(c, 1)).isEqualTo(LocalDate.of(2026, 9, 29));
        assertThat(ResumoService.rotulo(3)).isEqualTo("4º mês");
    }

    @Test
    void anonimizaNomeCompletoEPrimeiroNome() {
        assertThat(ResumoService.anonimizar("Ana Clara Souza riu. A ANA dormiu; Anabela não muda.", "Ana Clara Souza"))
                .isEqualTo("a criança riu. A a criança dormiu; Anabela não muda.");
        assertThat(ResumoService.anonimizar("sem nome", null)).isEqualTo("sem nome");
    }

    @Test
    void promptTemSoODoPeriodoSemNomeNemProfissional() {
        Crianca c = crianca();
        LocalDate hoje = LocalDate.of(2026, 11, 16); // 3 meses completos: o mês 2 (3º mês) já terminou
        MarcoAtingido m = new MarcoAtingido();
        m.setMarcoId("m03a");
        m.setAtingidoEm(LocalDate.of(2026, 11, 10));
        m.setNota("Ana gargalhou");
        VacinaAplicada v = new VacinaAplicada();
        v.setVacinaId("bcg");
        v.setAplicadaEm(NASC);
        var doses = VacinaService.montar(c, List.of(v), hoje).grupos().stream().flatMap(g -> g.doses().stream()).toList();
        var e = new ResumoService.Entrada(c, 2, hoje,
                List.of(peso(LocalDate.of(2026, 9, 20), 5.0, 40), peso(LocalDate.of(2026, 10, 15), 5.6, 45), peso(LocalDate.of(2026, 11, 5), 6.1, 50)),
                List.of(registro(LocalDate.of(2026, 10, 20), "Ana rolou na cama"), registro(LocalDate.of(2026, 11, 1), "fora do período")),
                List.of(m), doses, List.of());
        String p = ResumoService.prompt(e);
        assertThat(p).contains("3º mês de vida, de 30/09/2026 a 30/10/2026.") // nasceu num dia 31
                .doesNotContain("em andamento")
                .doesNotContain("Ana").doesNotContain("Fulana")
                .contains("a criança rolou na cama")
                .doesNotContain("fora do período")
                .contains("(antes do período) 20/09/2026")
                .contains("Ganho de peso: +600 g em 25 dias")
                .contains("sem aplicação registrada")
                .contains("menina")
                .doesNotContain("<dados>");
        assertThat(p).doesNotContain("gargalhou"); // marco de 10/11 é do mês seguinte
    }

    @Test
    void tiraLinksDosTextos() {
        assertThat(ResumoService.anonimizar("foto em https://casa/casa-api/v1/crescimento/fotos/abc?assinatura=x e www.site.com ok", "Ana"))
                .isEqualTo("foto em [link] e [link] ok");
        assertThat(ResumoService.semLinks("veja file:///dados/crescimento/x.jpg")).isEqualTo("veja [link]");
    }

    @Test
    void limitaGeracoesPorCrianca() {
        var relogio = new java.util.concurrent.atomic.AtomicReference<>(Instant.parse("2026-11-16T12:00:00Z"));
        java.time.Clock clock = new java.time.Clock() {
            public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
            public java.time.Clock withZone(java.time.ZoneId z) { return this; }
            public Instant instant() { return relogio.get(); }
        };
        ResumoService s = new ResumoService(null, null, null, null, null, null, null, null, null, null, clock);
        UUID a = UUID.randomUUID();
        for (int i = 0; i < ResumoService.LIMITE_DIA_POR_CRIANCA; i++) s.reservar(a);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> s.reservar(a))
                .isInstanceOf(com.kowalski.casaapi.crescimento.comum.IaIndisponivelException.class);
        s.reservar(UUID.randomUUID()); // outra criança não é afetada
        relogio.set(relogio.get().plus(java.time.Duration.ofHours(25)));
        s.reservar(a);
    }

    @Test
    void contextoDaPerguntaTemResumoEConversaAnterior() {
        ResumoMes rm = new ResumoMes();
        rm.setTexto("Mês tranquilo.");
        rm.setPerguntas("A cólica é normal?");
        PerguntaIa q = new PerguntaIa();
        q.setPergunta("Quanto ela deve dormir?");
        q.setResposta("Cerca de 14 a 17 horas.");
        String c = ResumoService.contextoPergunta("DADOS\n", rm, List.of(q));
        assertThat(c).startsWith("DADOS").contains("Resumo do mês já entregue aos pais:\nMês tranquilo.")
                .contains("A cólica é normal?").contains("Pergunta: Quanto ela deve dormir?\nResposta: Cerca de 14 a 17 horas.");
        assertThat(ResumoService.contextoPergunta("D", rm, List.of())).doesNotContain("Perguntas anteriores");
    }

    @Test
    void respostaMarcaParcialQuandoGeradoAntesDoFim() {
        ResumoMes r = new ResumoMes();
        r.setMes(2);
        r.setInicio(LocalDate.of(2026, 10, 31));
        r.setFim(LocalDate.of(2026, 11, 29));
        r.setTexto("t");
        r.setPerguntas("a?\nb?");
        r.setGeradoEm(Instant.parse("2026-11-16T12:00:00Z"));
        var resp = ResumoService.resposta(r);
        assertThat(resp.parcial()).isTrue();
        assertThat(resp.perguntas()).containsExactly("a?", "b?");
        assertThat(resp.rotulo()).isEqualTo("3º mês");
    }
}
