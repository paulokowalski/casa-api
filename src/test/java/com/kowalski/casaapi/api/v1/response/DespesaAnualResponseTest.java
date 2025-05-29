package com.kowalski.casaapi.api.v1.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DespesaAnualResponseTest {

    @Test
    @DisplayName("Deve criar DespesaAnualResponse com sucesso")
    void criarDespesaAnualResponse_Sucesso() {
        // Arrange
        String pessoa = "JOAO";
        String ano = "2024";
        Map<String, DespesaResponse> despesasPorMes = new HashMap<>();
        despesasPorMes.put("01", new DespesaResponse(
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(800),
            BigDecimal.valueOf(200),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(300)
        ));

        // Act
        DespesaAnualResponse response = new DespesaAnualResponse(pessoa, ano, despesasPorMes);

        // Assert
        assertThat(response)
            .isNotNull()
            .satisfies(r -> {
                assertThat(r.pessoa()).isEqualTo(pessoa);
                assertThat(r.ano()).isEqualTo(ano);
                assertThat(r.despesasPorMes())
                    .isNotNull()
                    .hasSize(1)
                    .containsKey("01");
            });
    }

    @Test
    @DisplayName("Deve aceitar valores nulos")
    void criarDespesaAnualResponse_ValoresNulos() {
        // Act
        DespesaAnualResponse response = new DespesaAnualResponse(null, null, null);

        // Assert
        assertThat(response)
            .isNotNull()
            .satisfies(r -> {
                assertThat(r.pessoa()).isNull();
                assertThat(r.ano()).isNull();
                assertThat(r.despesasPorMes()).isNull();
            });
    }

    @Test
    @DisplayName("Deve gerar toString corretamente")
    void toString_Sucesso() {
        // Arrange
        DespesaAnualResponse response = new DespesaAnualResponse(
            "JOAO",
            "2024",
            Map.of("01", new DespesaResponse(
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                BigDecimal.TEN
            ))
        );

        // Act
        String result = response.toString();

        // Assert
        assertThat(result)
            .contains("pessoa=JOAO")
            .contains("ano=2024")
            .contains("despesasPorMes={01=");
    }
} 