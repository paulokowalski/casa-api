package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;
import com.kowalski.casaapi.domain.service.FiltroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FiltroControllerTest {

    @Mock
    private FiltroService filtroService;

    @InjectMocks
    private FiltroController filtroController;

    private List<FiltroResponse> filtroResponses;

    @BeforeEach
    void setUp() {
        filtroResponses = List.of(
            new FiltroResponse("2024", "2024"),
            new FiltroResponse("2023", "2023")
        );
    }

    @Test
    @DisplayName("Deve buscar anos com sucesso")
    void buscarAnos_Sucesso() {
        // Arrange
        when(filtroService.buscarAnos()).thenReturn(filtroResponses);

        // Act
        ResponseEntity<List<FiltroResponse>> response = filtroController.buscarAnos();

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody())
            .isNotNull()
            .isEqualTo(filtroResponses);
        verify(filtroService).buscarAnos();
    }

    @Test
    @DisplayName("Deve buscar meses com sucesso")
    void buscarMeses_Sucesso() {
        // Arrange
        List<FiltroResponse> meses = List.of(
            new FiltroResponse("01", "JANEIRO"),
            new FiltroResponse("02", "FEVEREIRO")
        );
        when(filtroService.buscarMeses("2024")).thenReturn(meses);

        // Act
        ResponseEntity<List<FiltroResponse>> response = filtroController.buscarMeses("2024");

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody())
            .isNotNull()
            .isEqualTo(meses);
        verify(filtroService).buscarMeses("2024");
    }

    @Test
    @DisplayName("Deve buscar cartões com sucesso")
    void buscarCartoes_Sucesso() {
        // Arrange
        List<FiltroResponse> cartoes = List.of(
            new FiltroResponse("NUBANK", "NUBANK"),
            new FiltroResponse("MASTERCARD", "MASTERCARD")
        );
        when(filtroService.buscarCartoes("2024", "01", "JOAO")).thenReturn(cartoes);

        // Act
        ResponseEntity<List<FiltroResponse>> response = filtroController.buscarCartoes("2024", "01", "JOAO");

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody())
            .isNotNull()
            .isEqualTo(cartoes);
        verify(filtroService).buscarCartoes("2024", "01", "JOAO");
    }

    @Test
    @DisplayName("Deve buscar pessoas com sucesso")
    void buscarPessoas_Sucesso() {
        // Arrange
        List<FiltroResponse> pessoas = List.of(
            new FiltroResponse("JOAO", "JOAO"),
            new FiltroResponse("MARIA", "MARIA")
        );
        when(filtroService.buscarPessoas("2024", "01")).thenReturn(pessoas);

        // Act
        ResponseEntity<List<FiltroResponse>> response = filtroController.buscarPessoas("2024", "01");

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody())
            .isNotNull()
            .isEqualTo(pessoas);
        verify(filtroService).buscarPessoas("2024", "01");
    }

    @Test
    @DisplayName("Deve retornar 404 quando não encontrar anos")
    void buscarAnos_NotFound() {
        // Arrange
        when(filtroService.buscarAnos()).thenReturn(List.of());

        // Act
        ResponseEntity<List<FiltroResponse>> response = filtroController.buscarAnos();

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(404);
        verify(filtroService).buscarAnos();
    }

    @Test
    @DisplayName("Deve retornar 404 quando não encontrar meses")
    void buscarMeses_NotFound() {
        // Arrange
        when(filtroService.buscarMeses("2024")).thenReturn(List.of());

        // Act
        ResponseEntity<List<FiltroResponse>> response = filtroController.buscarMeses("2024");

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(404);
        verify(filtroService).buscarMeses("2024");
    }

    @Test
    @DisplayName("Deve retornar 404 quando não encontrar pessoas")
    void buscarPessoas_NotFound() {
        // Arrange
        when(filtroService.buscarPessoas("2024", "01")).thenReturn(List.of());

        // Act
        ResponseEntity<List<FiltroResponse>> response = filtroController.buscarPessoas("2024", "01");

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(404);
        verify(filtroService).buscarPessoas("2024", "01");
    }

    @Test
    @DisplayName("Deve retornar 404 quando não encontrar cartões")
    void buscarCartoes_NotFound() {
        // Arrange
        when(filtroService.buscarCartoes("2024", "01", "JOAO")).thenReturn(List.of());

        // Act
        ResponseEntity<List<FiltroResponse>> response = filtroController.buscarCartoes("2024", "01", "JOAO");

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(404);
        verify(filtroService).buscarCartoes("2024", "01", "JOAO");
    }
} 