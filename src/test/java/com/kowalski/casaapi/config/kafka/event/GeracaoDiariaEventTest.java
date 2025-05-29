package com.kowalski.casaapi.config.kafka.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeracaoDiariaEventTest {

    @Test
    @DisplayName("Deve criar evento com construtor vazio")
    void criarEvento_ConstrutorVazio() {
        // Act
        GeracaoDiariaEvent event = new GeracaoDiariaEvent();

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.getDia()).isNull();
        assertThat(event.getMes()).isNull();
        assertThat(event.getAno()).isNull();
    }

    @Test
    @DisplayName("Deve criar evento com todos os campos")
    void criarEvento_TodosCampos() {
        // Arrange
        String dia = "01";
        String mes = "02";
        String ano = "2024";

        // Act
        GeracaoDiariaEvent event = new GeracaoDiariaEvent(dia, mes, ano);

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.getDia()).isEqualTo(dia);
        assertThat(event.getMes()).isEqualTo(mes);
        assertThat(event.getAno()).isEqualTo(ano);
    }

    @Test
    @DisplayName("Deve usar setters corretamente")
    void setters_Sucesso() {
        // Arrange
        GeracaoDiariaEvent event = new GeracaoDiariaEvent();
        String dia = "01";
        String mes = "02";
        String ano = "2024";

        // Act
        event.setDia(dia);
        event.setMes(mes);
        event.setAno(ano);

        // Assert
        assertThat(event.getDia()).isEqualTo(dia);
        assertThat(event.getMes()).isEqualTo(mes);
        assertThat(event.getAno()).isEqualTo(ano);
    }

    @Test
    @DisplayName("Deve gerar toString corretamente")
    void toString_Sucesso() {
        // Arrange
        GeracaoDiariaEvent event = new GeracaoDiariaEvent("01", "02", "2024");

        // Act
        String result = event.toString();

        // Assert
        assertThat(result)
            .contains("dia=01")
            .contains("mes=02")
            .contains("ano=2024");
    }
} 