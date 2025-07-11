package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;
import com.kowalski.casaapi.business.dao.FiltroDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FiltroServiceImplTest {

    @Mock
    private FiltroDao filtroDao;

    @InjectMocks
    private FiltroServiceImpl filtroService;

    @Test
    @DisplayName("Deve buscar anos com sucesso")
    void buscarAnos_Sucesso() {
        // Arrange
        List<FiltroResponse> expected = List.of(new FiltroResponse("2024", "2024"));
        when(filtroDao.buscarAnos()).thenReturn(expected);

        // Act
        List<FiltroResponse> result = filtroService.buscarAnos();

        // Assert
        assertThat(result)
            .isNotNull()
            .isEqualTo(expected);
        verify(filtroDao).buscarAnos();
    }

    @Test
    @DisplayName("Deve buscar meses com sucesso")
    void buscarMeses_Sucesso() {
        // Arrange
        List<FiltroResponse> expected = List.of(new FiltroResponse("01", "JANEIRO"));
        when(filtroDao.buscarMeses("2024")).thenReturn(expected);

        // Act
        List<FiltroResponse> result = filtroService.buscarMeses("2024");

        // Assert
        assertThat(result)
            .isNotNull()
            .isEqualTo(expected);
        verify(filtroDao).buscarMeses("2024");
    }

    @Test
    @DisplayName("Deve buscar pessoas com sucesso")
    void buscarPessoas_Sucesso() {
        // Arrange
        List<FiltroResponse> expected = List.of(new FiltroResponse("JOAO", "JOAO"));
        when(filtroDao.buscarPessoas("2024", "01")).thenReturn(expected);

        // Act
        List<FiltroResponse> result = filtroService.buscarPessoas("2024", "01");

        // Assert
        assertThat(result)
            .isNotNull()
            .isEqualTo(expected);
        verify(filtroDao).buscarPessoas("2024", "01");
    }

    @Test
    @DisplayName("Deve buscar cartões com sucesso")
    void buscarCartoes_Sucesso() {
        // Arrange
        List<FiltroResponse> expected = List.of(new FiltroResponse("NUBANK", "NUBANK"));
        when(filtroDao.buscarCartoes("2024", "01", "JOAO")).thenReturn(expected);

        // Act
        List<FiltroResponse> result = filtroService.buscarCartoes("2024", "01", "JOAO");

        // Assert
        assertThat(result)
            .isNotNull()
            .isEqualTo(expected);
        verify(filtroDao).buscarCartoes("2024", "01", "JOAO");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar anos")
    void buscarAnos_ListaVazia() {
        // Arrange
        when(filtroDao.buscarAnos()).thenReturn(List.of());

        // Act
        List<FiltroResponse> result = filtroService.buscarAnos();

        // Assert
        assertThat(result).isEmpty();
        verify(filtroDao).buscarAnos();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar meses")
    void buscarMeses_ListaVazia() {
        // Arrange
        when(filtroDao.buscarMeses("2024")).thenReturn(List.of());

        // Act
        List<FiltroResponse> result = filtroService.buscarMeses("2024");

        // Assert
        assertThat(result).isEmpty();
        verify(filtroDao).buscarMeses("2024");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar pessoas")
    void buscarPessoas_ListaVazia() {
        // Arrange
        when(filtroDao.buscarPessoas("2024", "01")).thenReturn(List.of());

        // Act
        List<FiltroResponse> result = filtroService.buscarPessoas("2024", "01");

        // Assert
        assertThat(result).isEmpty();
        verify(filtroDao).buscarPessoas("2024", "01");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar cartões")
    void buscarCartoes_ListaVazia() {
        // Arrange
        when(filtroDao.buscarCartoes("2024", "01", "JOAO")).thenReturn(List.of());

        // Act
        List<FiltroResponse> result = filtroService.buscarCartoes("2024", "01", "JOAO");

        // Assert
        assertThat(result).isEmpty();
        verify(filtroDao).buscarCartoes("2024", "01", "JOAO");
    }
} 