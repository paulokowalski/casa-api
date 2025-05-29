package com.kowalski.casaapi.domain.dao.impl;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FiltroDaoImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private FiltroDaoImpl filtroDao;

    @BeforeEach
    void setUp() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    }

    @Test
    @DisplayName("Deve buscar anos com sucesso")
    void buscarAnos_Sucesso() {
        // Arrange
        Object[] row = new Object[]{"2024", "2024"};
        when(query.getResultList()).thenReturn(List.of(row));

        // Act
        List<FiltroResponse> result = filtroDao.buscarAnos();

        // Assert
        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .first()
            .satisfies(response -> {
                assertThat(response.codigo()).isEqualTo("2024");
                assertThat(response.descricao()).isEqualTo("2024");
            });
    }

    @Test
    @DisplayName("Deve buscar meses com sucesso")
    void buscarMeses_Sucesso() {
        // Arrange
        Object[] row = new Object[]{"1", "1"};
        when(query.getResultList()).thenReturn(List.of(row));

        // Act
        List<FiltroResponse> result = filtroDao.buscarMeses("2024");

        // Assert
        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .first()
            .satisfies(response -> {
                assertThat(response.codigo()).isEqualTo("01");
                assertThat(response.descricao()).isEqualTo("JANEIRO");
            });
    }

    @Test
    @DisplayName("Deve buscar pessoas com sucesso")
    void buscarPessoas_Sucesso() {
        // Arrange
        Object[] row = new Object[]{"JOAO", "JOAO"};
        when(query.getResultList()).thenReturn(List.of(row));

        // Act
        List<FiltroResponse> result = filtroDao.buscarPessoas("2024", "01");

        // Assert
        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .first()
            .satisfies(response -> {
                assertThat(response.codigo()).isEqualTo("JOAO");
                assertThat(response.descricao()).isEqualTo("JOAO");
            });
    }

    @Test
    @DisplayName("Deve buscar cartões com sucesso")
    void buscarCartoes_Sucesso() {
        // Arrange
        Object[] row = new Object[]{"NUBANK", "NUBANK"};
        when(query.getResultList()).thenReturn(List.of(row));

        // Act
        List<FiltroResponse> result = filtroDao.buscarCartoes("2024", "01", "JOAO");

        // Assert
        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .first()
            .satisfies(response -> {
                assertThat(response.codigo()).isEqualTo("NUBANK");
                assertThat(response.descricao()).isEqualTo("NUBANK");
            });
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar anos")
    void buscarAnos_ListaVazia() {
        // Arrange
        when(query.getResultList()).thenReturn(List.of());

        // Act
        List<FiltroResponse> result = filtroDao.buscarAnos();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar meses")
    void buscarMeses_ListaVazia() {
        // Arrange
        when(query.getResultList()).thenReturn(List.of());

        // Act
        List<FiltroResponse> result = filtroDao.buscarMeses("2024");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar pessoas")
    void buscarPessoas_ListaVazia() {
        // Arrange
        when(query.getResultList()).thenReturn(List.of());

        // Act
        List<FiltroResponse> result = filtroDao.buscarPessoas("2024", "01");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar cartões")
    void buscarCartoes_ListaVazia() {
        // Arrange
        when(query.getResultList()).thenReturn(List.of());

        // Act
        List<FiltroResponse> result = filtroDao.buscarCartoes("2024", "01", "JOAO");

        // Assert
        assertThat(result).isEmpty();
    }
} 