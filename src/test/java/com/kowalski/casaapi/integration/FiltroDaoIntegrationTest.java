package com.kowalski.casaapi.integration;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;
import com.kowalski.casaapi.domain.dao.FiltroDao;
import com.kowalski.casaapi.domain.model.Compra;
import com.kowalski.casaapi.domain.model.CompraParcela;
import com.kowalski.casaapi.domain.repository.CompraParcelaRepository;
import com.kowalski.casaapi.domain.repository.CompraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroDaoIntegrationTest extends IntegrationTestBase {

    @Autowired
    private FiltroDao filtroDao;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private CompraParcelaRepository compraParcelaRepository;

    private LocalDate dataAtual;
    private String anoAtual;
    private String mesAtual;

    @BeforeEach
    void setUp() {
        compraParcelaRepository.deleteAll();
        compraRepository.deleteAll();

        dataAtual = LocalDate.now();
        anoAtual = String.valueOf(dataAtual.getYear());
        mesAtual = String.format("%02d", dataAtual.getMonthValue());

        // Criar uma compra para teste
        Compra compra = Compra.builder()
            .id(UUID.randomUUID())
            .nomeProduto("PRODUTO TESTE")
            .valorProduto(BigDecimal.valueOf(1000))
            .dataCompra(dataAtual)
            .numeroParcelas(3)
            .nomePessoaCompra("JOAO")
            .nomeCartao("NUBANK")
            .dataCadastro(LocalDateTime.now())
            .build();
        compraRepository.save(compra);

        // Criar parcelas
        CompraParcela parcela = CompraParcela.builder()
            .id(UUID.randomUUID())
            .compra(compra)
            .numeroParcela(1)
            .valorParcela(BigDecimal.valueOf(333.33))
            .dataParcela(dataAtual)
            .build();
        compraParcelaRepository.save(parcela);
    }

    @Test
    @DisplayName("Deve buscar anos - Teste de Integração")
    void buscarAnos() {
        // Act
        List<FiltroResponse> result = filtroDao.buscarAnos();

        // Assert
        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .first()
            .satisfies(response -> {
                assertThat(response.codigo()).isEqualTo(anoAtual);
                assertThat(response.descricao()).isEqualTo(anoAtual);
            });
    }

    @Test
    @DisplayName("Deve buscar meses - Teste de Integração")
    void buscarMeses() {
        // Act
        List<FiltroResponse> result = filtroDao.buscarMeses(anoAtual);

        // Assert
        assertThat(result)
            .isNotNull()
            .hasSize(1)
            .first()
            .satisfies(response -> {
                assertThat(response.codigo()).isEqualTo(mesAtual);
                assertThat(response.descricao()).isNotBlank();
            });
    }

    @Test
    @DisplayName("Deve buscar pessoas - Teste de Integração")
    void buscarPessoas() {
        // Act
        List<FiltroResponse> result = filtroDao.buscarPessoas(anoAtual, mesAtual);

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
    @DisplayName("Deve buscar cartões - Teste de Integração")
    void buscarCartoes() {
        // Act
        List<FiltroResponse> result = filtroDao.buscarCartoes(anoAtual, mesAtual, "JOAO");

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
    @DisplayName("Deve retornar lista vazia quando não encontrar dados para o ano - Teste de Integração")
    void buscarMeses_AnoInexistente() {
        // Act
        List<FiltroResponse> result = filtroDao.buscarMeses("1900");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não encontrar dados para pessoa - Teste de Integração")
    void buscarCartoes_PessoaInexistente() {
        // Act
        List<FiltroResponse> result = filtroDao.buscarCartoes(anoAtual, mesAtual, "PESSOA_INEXISTENTE");

        // Assert
        assertThat(result).isEmpty();
    }
} 