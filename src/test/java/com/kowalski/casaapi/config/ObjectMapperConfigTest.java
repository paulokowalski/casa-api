package com.kowalski.casaapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectMapperConfigTest {

    private final ObjectMapperConfig config = new ObjectMapperConfig();

    @Test
    @DisplayName("Deve criar ObjectMapper configurado corretamente")
    void objectMapper_CriadoComSucesso() {
        // Act
        ObjectMapper mapper = config.objectMapper();

        // Assert
        assertThat(mapper).isNotNull();
        assertThat(mapper.canDeserialize(mapper.constructType(LocalDate.class))).isTrue();
    }

    @Test
    @DisplayName("Deve deserializar data no formato dd/MM/yyyy")
    void objectMapper_DeserializaDataCorretamente() throws Exception {
        // Arrange
        ObjectMapper mapper = config.objectMapper();
        String json = "\"01/02/2024\"";

        // Act
        LocalDate result = mapper.readValue(json, LocalDate.class);

        // Assert
        assertThat(result)
            .isNotNull()
            .satisfies(date -> {
                assertThat(date.getDayOfMonth()).isEqualTo(1);
                assertThat(date.getMonthValue()).isEqualTo(2);
                assertThat(date.getYear()).isEqualTo(2024);
            });
    }
} 