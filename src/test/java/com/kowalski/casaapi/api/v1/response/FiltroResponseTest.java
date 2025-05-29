package com.kowalski.casaapi.api.v1.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroResponseTest {

    @Test
    @DisplayName("Deve criar FiltroResponse com sucesso")
    void criarFiltroResponse_Sucesso() {
        // Arrange
        String codigo = "2024";
        String descricao = "2024";

        // Act
        FiltroResponse response = new FiltroResponse(codigo, descricao);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.codigo()).isEqualTo(codigo);
        assertThat(response.descricao()).isEqualTo(descricao);
    }

    @Test
    @DisplayName("Deve aceitar valores nulos")
    void criarFiltroResponse_ValoresNulos() {
        // Act
        FiltroResponse response = new FiltroResponse(null, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.codigo()).isNull();
        assertThat(response.descricao()).isNull();
    }

    @Test
    @DisplayName("Deve gerar toString corretamente")
    void toString_Sucesso() {
        // Arrange
        FiltroResponse response = new FiltroResponse("2024", "2024");

        // Act
        String result = response.toString();

        // Assert
        assertThat(result)
            .contains("codigo=2024")
            .contains("descricao=2024");
    }

    @Test
    @DisplayName("Deve comparar objetos corretamente")
    void equals_Sucesso() {
        // Arrange
        FiltroResponse response1 = new FiltroResponse("2024", "2024");
        FiltroResponse response2 = new FiltroResponse("2024", "2024");
        FiltroResponse response3 = new FiltroResponse("2023", "2023");

        // Assert
        assertThat(response1)
            .isEqualTo(response2)
            .isNotEqualTo(response3);
    }

    @Test
    @DisplayName("Deve gerar hashCode corretamente")
    void hashCode_Sucesso() {
        // Arrange
        FiltroResponse response1 = new FiltroResponse("2024", "2024");
        FiltroResponse response2 = new FiltroResponse("2024", "2024");

        // Assert
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }
} 