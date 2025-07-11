package com.kowalski.casaapi.integration;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import com.kowalski.casaapi.api.v1.response.DespesaAnualResponse;
import com.kowalski.casaapi.business.service.DespesaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class DespesaServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.3")
            .withDatabaseName("casa_api_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DespesaService despesaService;

    @Test
    @DisplayName("Deve buscar despesa por ano, mês e nome - Teste de Integração")
    void buscarPorAnoMesNome() {
        // Act
        DespesaResponse response = despesaService.buscarPorAnoMesNome("2024", "01", "joao");

        // Assert
        assertThat(response).isNull(); // Assumindo banco vazio inicialmente
    }

    @Test
    @DisplayName("Deve buscar despesas anuais - Teste de Integração")
    void buscarDespesasAnuais() {
        // Act
        DespesaAnualResponse response = despesaService.buscarDespesasAnuais("2024", "joao");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.pessoa()).isEqualTo("JOAO");
        assertThat(response.ano()).isEqualTo("2024");
        assertThat(response.despesasPorMes()).isEmpty(); // Assumindo banco vazio inicialmente
    }
} 