package com.kowalski.casaapi.api.v1.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DespesaResponseTest {

    @Test
    @DisplayName("Deve criar DespesaResponse com sucesso")
    void criarDespesaResponse_Sucesso() {
        // Arrange
        BigDecimal valorMes = BigDecimal.valueOf(1000);
        BigDecimal valorProximoMes = BigDecimal.valueOf(800);
        BigDecimal valorSaindo = BigDecimal.valueOf(200);
        BigDecimal valorParcelaSaindo = BigDecimal.valueOf(100);
        BigDecimal valorSaindoTotal = BigDecimal.valueOf(300);

        // Act
        DespesaResponse response = new DespesaResponse(
            valorMes,
            valorProximoMes,
            valorSaindo,
            valorParcelaSaindo,
            valorSaindoTotal
        );

        // Assert
        assertThat(response)
            .isNotNull()
            .satisfies(r -> {
                assertThat(r.valorMes()).isEqualByComparingTo(valorMes);
                assertThat(r.valorProximoMes()).isEqualByComparingTo(valorProximoMes);
                assertThat(r.valorSaindo()).isEqualByComparingTo(valorSaindo);
                assertThat(r.valorParcelaSaindo()).isEqualByComparingTo(valorParcelaSaindo);
                assertThat(r.valorSaindoTotal()).isEqualByComparingTo(valorSaindoTotal);
            });
    }

    @Test
    @DisplayName("Deve aceitar valores nulos")
    void criarDespesaResponse_ValoresNulos() {
        // Act
        DespesaResponse response = new DespesaResponse(null, null, null, null, null);

        // Assert
        assertThat(response)
            .isNotNull()
            .satisfies(r -> {
                assertThat(r.valorMes()).isNull();
                assertThat(r.valorProximoMes()).isNull();
                assertThat(r.valorSaindo()).isNull();
                assertThat(r.valorParcelaSaindo()).isNull();
                assertThat(r.valorSaindoTotal()).isNull();
            });
    }

    @Test
    @DisplayName("Deve gerar toString corretamente")
    void toString_Sucesso() {
        // Arrange
        DespesaResponse response = new DespesaResponse(
            BigDecimal.ONE,
            BigDecimal.TEN,
            BigDecimal.ZERO,
            BigDecimal.ONE,
            BigDecimal.TEN
        );

        // Act
        String result = response.toString();

        // Assert
        assertThat(result)
            .contains("valorMes=1")
            .contains("valorProximoMes=10")
            .contains("valorSaindo=0")
            .contains("valorParcelaSaindo=1")
            .contains("valorSaindoTotal=10");
    }
} 