package com.kowalski.casaapi.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompraParcelaTest {

    @Test
    @DisplayName("Deve criar CompraParcela com sucesso usando builder")
    void criarCompraParcela_Builder_Sucesso() {
        // Arrange
        UUID id = UUID.randomUUID();
        Compra compra = Compra.builder()
            .numeroParcelas(3)
            .build();
        Integer numeroParcela = 1;
        BigDecimal valorParcela = BigDecimal.valueOf(333.33);
        LocalDate dataParcela = LocalDate.now();

        // Act
        CompraParcela parcela = CompraParcela.builder()
            .id(id)
            .compra(compra)
            .numeroParcela(numeroParcela)
            .valorParcela(valorParcela)
            .dataParcela(dataParcela)
            .build();

        // Assert
        assertThat(parcela)
            .isNotNull()
            .satisfies(p -> {
                assertThat(p.getId()).isEqualTo(id);
                assertThat(p.getCompra()).isSameAs(compra);
                assertThat(p.getNumeroParcela()).isEqualTo(numeroParcela);
                assertThat(p.getValorParcela()).isEqualByComparingTo(valorParcela);
                assertThat(p.getDataParcela()).isEqualTo(dataParcela);
            });
    }

    @Test
    @DisplayName("Deve criar CompraParcela com construtor vazio")
    void criarCompraParcela_ConstrutorVazio() {
        // Act
        CompraParcela parcela = new CompraParcela();

        // Assert
        assertThat(parcela)
            .isNotNull()
            .satisfies(p -> {
                assertThat(p.getId()).isNull();
                assertThat(p.getCompra()).isNull();
                assertThat(p.getNumeroParcela()).isNull();
                assertThat(p.getValorParcela()).isNull();
                assertThat(p.getDataParcela()).isNull();
            });
    }

    @Test
    @DisplayName("Deve usar setters corretamente")
    void setters_Sucesso() {
        // Arrange
        CompraParcela parcela = new CompraParcela();
        UUID id = UUID.randomUUID();
        Compra compra = new Compra();
        Integer numeroParcela = 1;
        BigDecimal valorParcela = BigDecimal.valueOf(333.33);
        LocalDate dataParcela = LocalDate.now();

        // Act
        parcela.setId(id);
        parcela.setCompra(compra);
        parcela.setNumeroParcela(numeroParcela);
        parcela.setValorParcela(valorParcela);
        parcela.setDataParcela(dataParcela);

        // Assert
        assertThat(parcela)
            .satisfies(p -> {
                assertThat(p.getId()).isEqualTo(id);
                assertThat(p.getCompra()).isSameAs(compra);
                assertThat(p.getNumeroParcela()).isEqualTo(numeroParcela);
                assertThat(p.getValorParcela()).isEqualByComparingTo(valorParcela);
                assertThat(p.getDataParcela()).isEqualTo(dataParcela);
            });
    }

    @Test
    @DisplayName("Deve identificar última parcela corretamente")
    void isUltimaParcela_Sucesso() {
        // Arrange
        Compra compra = Compra.builder()
            .numeroParcelas(3)
            .build();

        CompraParcela parcela1 = CompraParcela.builder()
            .compra(compra)
            .numeroParcela(1)
            .build();

        CompraParcela parcela3 = CompraParcela.builder()
            .compra(compra)
            .numeroParcela(3)
            .build();

        // Assert
        assertThat(parcela1.isUltimaParcela()).isFalse();
        assertThat(parcela3.isUltimaParcela()).isTrue();
    }
} 