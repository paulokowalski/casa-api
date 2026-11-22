package com.kowalski.casaapi.crescimento.diario;

import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.medida.Medicao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LinhaDoTempoTest {

    static Medicao m(String data, Double peso) {
        Medicao m = new Medicao();
        m.setId(UUID.randomUUID());
        m.setData(LocalDate.parse(data));
        m.setPesoKg(peso);
        return m;
    }

    @Test
    void ganhoComparaComAPesagemAnteriorIgnorandoMedicoesSemPeso() {
        Medicao a = m("2026-01-10", 3.3), semPeso = m("2026-01-20", null), b = m("2026-02-10", 4.25);
        var g = RegistroService.ganhos(List.of(b, semPeso, a));
        assertThat(g).doesNotContainKey(a.getId()).doesNotContainKey(semPeso.getId());
        assertThat(g.get(b.getId())).containsExactly(950, 31);
    }

    @Test
    void mesDeVida() {
        Crianca c = new Crianca();
        c.setNascimento(LocalDate.of(2026, 1, 31));
        assertThat(RegistroService.mesDeVida(c, LocalDate.of(2026, 2, 27))).isZero();
        assertThat(RegistroService.mesDeVida(c, LocalDate.of(2026, 2, 28))).isEqualTo(1);
        assertThat(RegistroService.mesDeVida(c, LocalDate.of(2027, 3, 1))).isEqualTo(13);
    }
}
