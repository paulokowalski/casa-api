package com.kowalski.casaapi.comum;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompetenciasTest {

    @Test
    void converteDeEParaTexto() {
        assertThat(Competencias.parse(" 2026-11 ", "de")).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(Competencias.formatar(LocalDate.of(2026, 2, 1))).isEqualTo("2026-02");
        assertThat(Competencias.formatar(null)).isNull();
        assertThat(Competencias.parseOuNulo(" ", "x")).isNull();
    }

    @Test
    void formatoInvalidoDizQualCampo() {
        assertThatThrownBy(() -> Competencias.parse("11/2026", "ate"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ate");
        assertThatThrownBy(() -> Competencias.parse(null, "de")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Competencias.de(2026, 13)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void vencimentoUsaOUltimoDiaQuandoODiaNaoExiste() {
        assertThat(Competencias.vencimento(LocalDate.of(2026, 2, 1), 31)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(Competencias.vencimento(LocalDate.of(2028, 2, 1), 30)).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(Competencias.vencimento(LocalDate.of(2026, 3, 1), 10)).isEqualTo(LocalDate.of(2026, 3, 10));
    }

    @Test
    void competenciaAtualUsaOFusoDaCasa() {
        Clock primeiroMinutoDeDezembroEmUtc = Clock.fixed(Instant.parse("2026-12-01T00:30:00Z"), RelogioConfig.FUSO_DA_CASA);

        assertThat(Competencias.atual(primeiroMinutoDeDezembroEmUtc)).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void mesesEntreIgnoraODia() {
        assertThat(Competencias.mesesEntre(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 1))).isEqualTo(2);
    }
}
