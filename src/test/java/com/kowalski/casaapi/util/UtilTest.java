package com.kowalski.casaapi.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UtilTest {

    @ParameterizedTest
    @CsvSource({
        "João,Joao",
        "Não,Nao",
        "Café,Cafe",
        "Açúcar,Acucar",
        "Maçã,Maca",
        "JOSÉ,JOSE",
        "ÚLTIMA,ULTIMA"
    })
    @DisplayName("Deve remover acentos corretamente")
    void removerAcentos_Sucesso(String entrada, String esperado) {
        // Act
        String resultado = Util.removerAcentos(entrada);

        // Assert
        assertThat(resultado).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Deve manter texto sem acentos inalterado")
    void removerAcentos_TextoSemAcentos() {
        // Arrange
        String texto = "Texto sem acentos 123";

        // Act
        String resultado = Util.removerAcentos(texto);

        // Assert
        assertThat(resultado).isEqualTo(texto);
    }

    @Test
    @DisplayName("Deve lidar com string vazia")
    void removerAcentos_StringVazia() {
        // Act
        String resultado = Util.removerAcentos("");

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve lidar com caracteres especiais")
    void removerAcentos_CaracteresEspeciais() {
        // Arrange
        String texto = "!@#$%¨&*()_+";

        // Act
        String resultado = Util.removerAcentos(texto);

        // Assert
        assertThat(resultado).isEqualTo(texto);
    }
} 