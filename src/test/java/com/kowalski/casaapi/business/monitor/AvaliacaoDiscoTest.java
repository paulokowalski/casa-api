package com.kowalski.casaapi.business.monitor;

import com.kowalski.casaapi.api.v1.response.ServidorSaudeDiscoResponse;
import com.kowalski.casaapi.business.monitor.AvaliacaoDisco.Nivel;
import com.kowalski.casaapi.business.monitor.AvaliacaoDisco.Situacao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Casos montados com as leituras reais dos discos do servidor. */
class AvaliacaoDiscoTest {

    private static SaudeSmart hd(Boolean aprovado, Double maxVida, Double limite, Double absoluto, Long vezesAcima,
                                 Map<Integer, Long> atributos, List<SaudeSmart.Falha> falhas) {
        return new SaudeSmart(null, aprovado, 1000L, 10L, 20.0, maxVida, limite, absoluto, vezesAcima, atributos,
                falhas, null);
    }

    private static SaudeSmart nvme(SaudeSmart.Nvme n) {
        return new SaudeSmart(null, true, 27607L, 492L, null, null, null, null, null, Map.of(), List.of(), n);
    }

    @Test
    void discoSemNadaEhBom() {
        var r = AvaliacaoDisco.avaliar(hd(true, 45.0, 60.0, 70.0, 0L, Map.of(5, 0L, 197, 0L, 199, 0L), List.of()), 40.0);
        assertThat(r.situacao()).isEqualTo(Situacao.BOA);
        assertThat(r.alertas()).isEmpty();
    }

    @Test
    void seagateComErrosNaoCorrigidosEhCritico() {
        var s = hd(true, 61.0, 55.0, 60.0, 106L, Map.of(5, 0L, 187, 56_928L, 199, 18L),
                List.of(new SaudeSmart.Falha("Airflow_Temperature_Cel", "past")));

        var r = AvaliacaoDisco.avaliar(s, 54.0);

        assertThat(r.situacao()).isEqualTo(Situacao.CRITICA);
        assertThat(r.alertas().get(0).nivel()).isEqualTo(Nivel.CRITICO);       // o mais grave primeiro
        assertThat(r.alertas().get(0).mensagem()).contains("56.928");
        assertThat(r.alertas()).extracting(AvaliacaoDisco.Alerta::nivel)
                .contains(Nivel.ATENCAO, Nivel.INFO);                            // temperatura passada e CRC
        assertThat(r.alertas().get(r.alertas().size() - 1).mensagem()).contains("cabo");
    }

    @Test
    void poucosSetoresRealocadosEhAtencao() {
        var r = AvaliacaoDisco.avaliar(hd(true, 60.0, 55.0, 60.0, 0L, Map.of(5, 26L), List.of()), 50.0);
        assertThat(r.situacao()).isEqualTo(Situacao.ATENCAO);
        assertThat(r.alertas()).singleElement().satisfies(a -> assertThat(a.mensagem()).startsWith("26 setor"));
    }

    @Test
    void setorPendenteOuReprovadoEhCritico() {
        assertThat(AvaliacaoDisco.avaliar(hd(true, null, null, null, null, Map.of(197, 1L), List.of()), null).situacao())
                .isEqualTo(Situacao.CRITICA);
        assertThat(AvaliacaoDisco.avaliar(hd(false, null, null, null, null, Map.of(), List.of()), null).situacao())
                .isEqualTo(Situacao.CRITICA);
        assertThat(AvaliacaoDisco.avaliar(hd(true, null, null, null, null, Map.of(),
                List.of(new SaudeSmart.Falha("Reallocated_Sector_Ct", "now"))), null).situacao())
                .isEqualTo(Situacao.CRITICA);
    }

    @Test
    void temperaturaAtualContraOLimiteDoFabricante() {
        var s = hd(true, 50.0, 55.0, 60.0, 0L, Map.of(), List.of());
        assertThat(AvaliacaoDisco.avaliar(s, 54.0).situacao()).isEqualTo(Situacao.BOA);
        assertThat(AvaliacaoDisco.avaliar(s, 56.0).situacao()).isEqualTo(Situacao.ATENCAO);
        assertThat(AvaliacaoDisco.avaliar(s, 60.0).situacao()).isEqualTo(Situacao.CRITICA);
        assertThat(AvaliacaoDisco.avaliar(s, null).situacao()).isEqualTo(Situacao.BOA); // em repouso
    }

    @Test
    void poucosErrosDeComunicacaoSoInformam() {
        var r = AvaliacaoDisco.avaliar(hd(true, null, null, null, null, Map.of(199, 3L), List.of()), null);
        assertThat(r.situacao()).isEqualTo(Situacao.BOA);
        assertThat(r.alertas()).singleElement().satisfies(a -> assertThat(a.nivel()).isEqualTo(Nivel.INFO));
    }

    @Test
    void nvmeDoServidor() {
        var n = new SaudeSmart.Nvme(0L, 100L, 10L, 5L, 0L, 0L, 426L, 23L, 0L, 11_526_673_920_000L, 8_169_288_192_000L);
        var r = AvaliacaoDisco.avaliar(nvme(n), 45.0);
        assertThat(r.situacao()).isEqualTo(Situacao.ATENCAO); // 23 minutos acima da temperatura de aviso
        assertThat(r.alertas()).extracting(AvaliacaoDisco.Alerta::nivel).containsExactly(Nivel.ATENCAO, Nivel.INFO);
    }

    @Test
    void nvmeGastoOuSemReservaEhCritico() {
        var gasto = new SaudeSmart.Nvme(0L, 100L, 10L, 100L, 0L, 0L, 0L, 0L, 0L, null, null);
        var semReserva = new SaudeSmart.Nvme(1L, 5L, 10L, 30L, 0L, 0L, 0L, 0L, 0L, null, null);
        assertThat(AvaliacaoDisco.avaliar(nvme(gasto), null).situacao()).isEqualTo(Situacao.CRITICA);
        assertThat(AvaliacaoDisco.avaliar(nvme(semReserva), null).alertas()).hasSize(2);
    }

    @Test
    void respostaUsaNomesEmMinusculasEEscondeMinimaInvalida() {
        var s = new SaudeSmart(null, true, 1L, 1L, 0.0, 61.0, 60.0, 85.0, 5L, Map.of(193, 54_622L), List.of(), null);
        var resp = ServidorSaudeDiscoResponse.de(s, 51.0);
        assertThat(resp.situacao()).isEqualTo("atencao");
        assertThat(resp.alertas()).singleElement().satisfies(a -> assertThat(a.nivel()).isEqualTo("atencao"));
        assertThat(resp.temperaturaMinVida()).isNull();
        assertThat(resp.ciclosCabeca()).isEqualTo(54_622L);
        assertThat(ServidorSaudeDiscoResponse.de(null, 40.0)).isNull();
    }
}
