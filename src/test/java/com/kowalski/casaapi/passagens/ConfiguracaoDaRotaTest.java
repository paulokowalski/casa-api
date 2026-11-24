package com.kowalski.casaapi.passagens;

import com.kowalski.casaapi.passagens.RotaPassagemInput.Data;
import com.kowalski.casaapi.passagens.RotaPassagemInput.Janela;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracaoDaRotaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 21);

    private static RotaPassagemInput rota(List<String> origem, List<String> destino, List<Data> datas, Janela janela) {
        return new RotaPassagemInput("  Fortaleza   - SP ", null, origem, destino, datas, janela, 1000, 1, null, null, null, null, null);
    }

    private static RotaPassagemInput comPassageiros(int adultos, int criancas, int bebesAssento, int bebesColo) {
        return new RotaPassagemInput("Família", null, List.of("FOR"), List.of("GRU"),
                List.of(new Data(LocalDate.of(2027, 9, 12), null)), null, null, 1, null,
                adultos, criancas, bebesAssento, bebesColo);
    }

    @Test
    void criancasEBebes() {
        Map<String, Object> cfg = ConfiguracaoDaRota.validar(comPassageiros(2, 1, 0, 1), HOJE);
        assertEquals(2, cfg.get("adultos"));
        assertEquals(1, cfg.get("criancas"));
        assertEquals(0, cfg.get("bebesAssento"));
        assertEquals(1, cfg.get("bebesColo"));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracaoDaRota.validar(comPassageiros(1, 0, 0, 2), HOJE));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracaoDaRota.validar(comPassageiros(5, 3, 2, 0), HOJE));
    }

    @Test
    void normalizaEPreencheOsPadroes() {
        Map<String, Object> cfg = ConfiguracaoDaRota.validar(rota(List.of(" for "), List.of("gru", "CGH", "GRU"),
                List.of(new Data(LocalDate.of(2027, 9, 12), LocalDate.of(2027, 9, 18))), null), HOJE);
        assertEquals("Fortaleza - SP", cfg.get("nome"));
        assertEquals(List.of("FOR"), cfg.get("origem"));
        assertEquals(List.of("GRU", "CGH"), cfg.get("destino"));
        assertEquals(true, cfg.get("ativa"));
        assertEquals(false, cfg.get("somenteIda"));
        assertEquals(25, cfg.get("quedaPct"));
        assertEquals(1, cfg.get("adultos"));
        assertEquals(0, cfg.get("criancas"));
        assertEquals(0, cfg.get("bebesColo"));
    }

    @Test
    void soIdaQuandoNaoHaVolta() {
        Map<String, Object> cfg = ConfiguracaoDaRota.validar(rota(List.of("GRU"), List.of("FOR"),
                List.of(new Data(LocalDate.of(2027, 9, 18), null)), null), HOJE);
        assertTrue((Boolean) cfg.get("somenteIda"));
    }

    @Test
    void janelaComDuracaoEIdaEVolta() {
        Map<String, Object> cfg = ConfiguracaoDaRota.validar(rota(List.of("GRU"), List.of("LIS"), null,
                new Janela(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31), 2, List.of(7, 10), List.of(4, 4, 0))), HOJE);
        assertEquals(false, cfg.get("somenteIda"));
        @SuppressWarnings("unchecked")
        Map<String, Object> j = (Map<String, Object>) cfg.get("janela");
        assertEquals(List.of(0, 4), j.get("diasSemana"));
    }

    @Test
    void recusaDadosInvalidos() {
        Data ok = new Data(LocalDate.of(2027, 9, 12), null);
        assertThrows(IllegalArgumentException.class, () -> ConfiguracaoDaRota.validar(
                rota(List.of("FORT"), List.of("GRU"), List.of(ok), null), HOJE));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracaoDaRota.validar(
                rota(List.of("GRU"), List.of("GRU"), List.of(ok), null), HOJE));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracaoDaRota.validar(
                rota(List.of("FOR"), List.of("GRU"), List.of(new Data(HOJE, null)), null), HOJE));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracaoDaRota.validar(
                rota(List.of("FOR"), List.of("GRU"),
                        List.of(new Data(LocalDate.of(2027, 9, 12), LocalDate.of(2027, 9, 10))), null), HOJE));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracaoDaRota.validar(
                rota(List.of("FOR"), List.of("GRU"), List.of(ok),
                        new Janela(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 2), 1, null, null)), HOJE));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracaoDaRota.validar(
                rota(List.of("FOR"), List.of("GRU"), List.of(), null), HOJE));
    }

    @Test
    void diaDaSemanaNuloEhRecusadoCom400() {
        java.util.List<Integer> comNulo = new java.util.ArrayList<>(List.of(1));
        comNulo.add(null);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ConfiguracaoDaRota.validar(
                rota(List.of("FOR"), List.of("GRU"), null,
                        new Janela(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31), 1, null, comNulo)), HOJE));
        assertTrue(e.getMessage().contains("Dia da semana"));
    }
}
