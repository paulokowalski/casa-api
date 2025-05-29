package com.kowalski.casaapi.domain.enumerator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MesesTest {

    @Test
    @DisplayName("Deve conter todos os meses do ano na ordem correta")
    void meses_OrdemCorreta() {
        // Assert
        assertThat(Meses.values())
            .hasSize(13) // 12 meses + VAZIO
            .containsExactly(
                Meses.VAZIO,
                Meses.JANEIRO,
                Meses.FEVEREIRO,
                Meses.MARCO,
                Meses.ABRIL,
                Meses.MAIO,
                Meses.JUNHO,
                Meses.JULHO,
                Meses.AGOSTO,
                Meses.SETEMBRO,
                Meses.OUTUBRO,
                Meses.NOVEMBRO,
                Meses.DEZEMBRO
            );
    }

    @Test
    @DisplayName("Deve retornar o nome do mês em maiúsculas")
    void meses_NomeMaiusculo() {
        // Assert
        for (Meses mes : Meses.values()) {
            assertThat(mes.name())
                .isUpperCase()
                .doesNotContainAnyWhitespaces();
        }
    }
} 