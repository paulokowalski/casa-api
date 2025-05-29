package com.kowalski.casaapi.config.modelmapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ModelMapperConfigTest {

    private final ModelMapperConfig config = new ModelMapperConfig();

    @Test
    @DisplayName("Deve criar ModelMapper com sucesso")
    void modelMapper_CriadoComSucesso() {
        // Act
        ModelMapper mapper = config.modelMapper();

        // Assert
        assertThat(mapper).isNotNull();
    }
} 