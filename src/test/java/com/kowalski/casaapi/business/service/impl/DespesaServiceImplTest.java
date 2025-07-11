package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import com.kowalski.casaapi.api.v1.response.DespesaAnualResponse;
import com.kowalski.casaapi.business.dao.DespesaDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DespesaServiceImplTest {

    @Mock
    private DespesaDao despesaDao;

    @InjectMocks
    private DespesaServiceImpl despesaService;

    private DespesaResponse despesaResponse;

    @BeforeEach
    void setUp() {
        despesaResponse = new DespesaResponse(
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(900),
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(150)
        );
    }

    @Test
    @DisplayName("Deve buscar despesa por ano, mês e nome com sucesso")
    void buscarPorAnoMesNome_Sucesso() {
        // Arrange
        when(despesaDao.findByNomePessoaAndMesAnoReferencia("JOAO", "01", "2024"))
            .thenReturn(despesaResponse);

        // Act
        DespesaResponse response = despesaService.buscarPorAnoMesNome("2024", "01", "joao");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.valorMes()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        verify(despesaDao).findByNomePessoaAndMesAnoReferencia("JOAO", "01", "2024");
    }

    @Test
    @DisplayName("Deve retornar null quando não encontrar despesa")
    void buscarPorAnoMesNome_NaoEncontrado() {
        // Arrange
        when(despesaDao.findByNomePessoaAndMesAnoReferencia(anyString(), anyString(), anyString()))
            .thenReturn(null);

        // Act
        DespesaResponse response = despesaService.buscarPorAnoMesNome("2024", "01", "joao");

        // Assert
        assertThat(response).isNull();
    }

    @Test
    @DisplayName("Deve lançar exceção quando ano estiver vazio")
    void buscarPorAnoMesNome_AnoVazio() {
        assertThatThrownBy(() -> despesaService.buscarPorAnoMesNome("", "01", "joao"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("O ano não pode estar vazio");
    }

    @Test
    @DisplayName("Deve buscar despesas anuais com sucesso")
    void buscarDespesasAnuais_Sucesso() {
        // Arrange
        when(despesaDao.findByNomePessoaAndMesAnoReferencia("JOAO", "01", "2024"))
            .thenReturn(despesaResponse);
        when(despesaDao.findByNomePessoaAndMesAnoReferencia("JOAO", "02", "2024"))
            .thenReturn(despesaResponse);

        // Act
        DespesaAnualResponse response = despesaService.buscarDespesasAnuais("2024", "joao");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.pessoa()).isEqualTo("JOAO");
        assertThat(response.ano()).isEqualTo("2024");
        assertThat(response.despesasPorMes())
            .isNotEmpty()
            .hasSize(2)
            .containsKeys("01", "02");

        Map<String, DespesaResponse> despesas = response.despesasPorMes();
        assertThat(despesas.get("01").valorMes()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(despesas.get("02").valorMes()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("Deve retornar mapa vazio quando não encontrar despesas no ano")
    void buscarDespesasAnuais_SemDespesas() {
        // Arrange
        when(despesaDao.findByNomePessoaAndMesAnoReferencia(anyString(), anyString(), anyString()))
            .thenReturn(null);

        // Act
        DespesaAnualResponse response = despesaService.buscarDespesasAnuais("2024", "joao");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.pessoa()).isEqualTo("JOAO");
        assertThat(response.ano()).isEqualTo("2024");
        assertThat(response.despesasPorMes()).isEmpty();
    }

    @Test
    @DisplayName("Deve manter ordem crescente dos meses")
    void buscarDespesasAnuais_OrdemCrescente() {
        // Arrange
        when(despesaDao.findByNomePessoaAndMesAnoReferencia("JOAO", "12", "2024"))
            .thenReturn(despesaResponse);
        when(despesaDao.findByNomePessoaAndMesAnoReferencia("JOAO", "01", "2024"))
            .thenReturn(despesaResponse);
        when(despesaDao.findByNomePessoaAndMesAnoReferencia("JOAO", "06", "2024"))
            .thenReturn(despesaResponse);

        // Act
        DespesaAnualResponse response = despesaService.buscarDespesasAnuais("2024", "joao");

        // Assert
        assertThat(response.despesasPorMes().keySet())
            .containsExactly("01", "06", "12");
    }
} 