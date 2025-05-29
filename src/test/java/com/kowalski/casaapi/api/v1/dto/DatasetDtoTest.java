package com.kowalski.casaapi.api.v1.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetDtoTest {

    @Test
    @DisplayName("Deve criar DatasetDto com sucesso")
    void criarDatasetDto_Sucesso() {
        // Arrange
        List<Double> data = List.of(1.0, 2.0, 3.0);

        // Act
        DatasetDto dto = new DatasetDto(data);

        // Assert
        assertThat(dto.getData())
            .isNotNull()
            .hasSize(3)
            .containsExactly(1.0, 2.0, 3.0);
    }

    @Test
    @DisplayName("Deve aceitar lista vazia")
    void criarDatasetDto_ListaVazia() {
        // Act
        DatasetDto dto = new DatasetDto(List.of());

        // Assert
        assertThat(dto.getData()).isEmpty();
    }

    @Test
    @DisplayName("Deve aceitar lista nula")
    void criarDatasetDto_ListaNula() {
        // Act
        DatasetDto dto = new DatasetDto(null);

        // Assert
        assertThat(dto.getData()).isNull();
    }
} 