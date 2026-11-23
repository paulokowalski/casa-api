package com.kowalski.casaapi.ventilacao;

import com.kowalski.casaapi.ventilacao.VentilacaoResponse.Leitura;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VentilacaoTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 21, 14, 0);

    private static VentilacaoRepository.Minuto minuto(int min, String tomada, Double hds) {
        return new VentilacaoRepository.Minuto(INICIO.plusMinutes(min),
                new Leitura(tomada, null, true, 45.0, hds, 50.0, null, null, null));
    }

    @Test
    void juntaMinutosLigadosSeguidosEmUmPeriodo() {
        List<VentilacaoRepository.Minuto> m = new ArrayList<>();
        for (int i = 0; i < 10; i++) m.add(minuto(i, i < 3 || i >= 7 ? "off" : "on", 41.0));
        List<VentilacaoResponse.Periodo> p = VentilacaoService.periodos(m);
        assertThat(p).containsExactly(new VentilacaoResponse.Periodo(INICIO.plusMinutes(3), INICIO.plusMinutes(7), 4));
    }

    @Test
    void falhaCurtaNaLeituraNaoQuebraOPeriodoMasFalhaLongaSim() {
        List<VentilacaoRepository.Minuto> m = List.of(minuto(0, "on", 41.0), minuto(1, "on", 41.0),
                minuto(4, "on", 41.0), minuto(20, "on", 41.0));
        List<VentilacaoResponse.Periodo> p = VentilacaoService.periodos(m);
        assertThat(p).containsExactly(
                new VentilacaoResponse.Periodo(INICIO, INICIO.plusMinutes(5), 3),
                new VentilacaoResponse.Periodo(INICIO.plusMinutes(20), INICIO.plusMinutes(21), 1));
    }

    @Test
    void periodoAindaLigadoTerminaNoUltimoMinuto() {
        List<VentilacaoResponse.Periodo> p = VentilacaoService.periodos(List.of(minuto(0, "off", 41.0),
                minuto(1, "on", 41.0), minuto(2, "on", 41.0)));
        assertThat(p).containsExactly(new VentilacaoResponse.Periodo(INICIO.plusMinutes(1), INICIO.plusMinutes(3), 2));
    }

    @Test
    void tomadaForaDoArFicaSemValorEHdsEmRepousoNaoViraZero() {
        List<VentilacaoResponse.Ponto> p = VentilacaoService.pontos(List.of(minuto(0, "unavailable", 0.0),
                minuto(1, "on", 42.0)));
        assertThat(p).containsExactly(new VentilacaoResponse.Ponto("14:00", null, 45.0, null, 50.0),
                new VentilacaoResponse.Ponto("14:01", 1, 45.0, 42.0, 50.0));
    }
}
