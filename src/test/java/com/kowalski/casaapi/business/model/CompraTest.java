package com.kowalski.casaapi.business.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompraTest {

    @Test
    @DisplayName("Deve criar Compra com sucesso usando builder")
    void criarCompra_Builder_Sucesso() {
        // Arrange
        UUID id = UUID.randomUUID();
        String nomeProduto = "Produto Teste";
        BigDecimal valorProduto = BigDecimal.valueOf(1000);
        LocalDate dataCompra = LocalDate.now();
        Integer numeroParcelas = 3;
        String nomePessoaCompra = "JOAO";
        String nomeCartao = "NUBANK";
        LocalDateTime dataCadastro = LocalDateTime.now();
        List<CompraParcela> parcelas = new ArrayList<>();
        Cartao cartao = new Cartao();

        // Act
        Compra compra = Compra.builder()
            .id(id)
            .nomeProduto(nomeProduto)
            .valorProduto(valorProduto)
            .dataCompra(dataCompra)
            .numeroParcelas(numeroParcelas)
            .nomePessoaCompra(nomePessoaCompra)
            .nomeCartao(nomeCartao)
            .dataCadastro(dataCadastro)
            .parcelas(parcelas)
            .cartao(cartao)
            .build();

        // Assert
        assertThat(compra)
            .isNotNull()
            .satisfies(c -> {
                assertThat(c.getId()).isEqualTo(id);
                assertThat(c.getNomeProduto()).isEqualTo(nomeProduto);
                assertThat(c.getValorProduto()).isEqualByComparingTo(valorProduto);
                assertThat(c.getDataCompra()).isEqualTo(dataCompra);
                assertThat(c.getNumeroParcelas()).isEqualTo(numeroParcelas);
                assertThat(c.getNomePessoaCompra()).isEqualTo(nomePessoaCompra);
                assertThat(c.getNomeCartao()).isEqualTo(nomeCartao);
                assertThat(c.getDataCadastro()).isEqualTo(dataCadastro);
                assertThat(c.getParcelas()).isSameAs(parcelas);
                assertThat(c.getCartao()).isSameAs(cartao);
            });
    }

    @Test
    @DisplayName("Deve criar Compra com construtor vazio")
    void criarCompra_ConstrutorVazio() {
        // Act
        Compra compra = new Compra();

        // Assert
        assertThat(compra)
            .isNotNull()
            .satisfies(c -> {
                assertThat(c.getId()).isNull();
                assertThat(c.getNomeProduto()).isNull();
                assertThat(c.getValorProduto()).isNull();
                assertThat(c.getDataCompra()).isNull();
                assertThat(c.getNumeroParcelas()).isNull();
                assertThat(c.getNomePessoaCompra()).isNull();
                assertThat(c.getNomeCartao()).isNull();
                assertThat(c.getDataCadastro()).isNull();
                assertThat(c.getParcelas()).isNull();
                assertThat(c.getCartao()).isNull();
            });
    }

    @Test
    @DisplayName("Deve usar setters corretamente")
    void setters_Sucesso() {
        // Arrange
        Compra compra = new Compra();
        UUID id = UUID.randomUUID();
        String nomeProduto = "Produto Teste";
        BigDecimal valorProduto = BigDecimal.valueOf(1000);
        LocalDate dataCompra = LocalDate.now();
        Integer numeroParcelas = 3;
        String nomePessoaCompra = "JOAO";
        String nomeCartao = "NUBANK";
        LocalDateTime dataCadastro = LocalDateTime.now();
        List<CompraParcela> parcelas = new ArrayList<>();
        Cartao cartao = new Cartao();

        // Act
        compra.setId(id);
        compra.setNomeProduto(nomeProduto);
        compra.setValorProduto(valorProduto);
        compra.setDataCompra(dataCompra);
        compra.setNumeroParcelas(numeroParcelas);
        compra.setNomePessoaCompra(nomePessoaCompra);
        compra.setNomeCartao(nomeCartao);
        compra.setDataCadastro(dataCadastro);
        compra.setParcelas(parcelas);
        compra.setCartao(cartao);

        // Assert
        assertThat(compra)
            .satisfies(c -> {
                assertThat(c.getId()).isEqualTo(id);
                assertThat(c.getNomeProduto()).isEqualTo(nomeProduto);
                assertThat(c.getValorProduto()).isEqualByComparingTo(valorProduto);
                assertThat(c.getDataCompra()).isEqualTo(dataCompra);
                assertThat(c.getNumeroParcelas()).isEqualTo(numeroParcelas);
                assertThat(c.getNomePessoaCompra()).isEqualTo(nomePessoaCompra);
                assertThat(c.getNomeCartao()).isEqualTo(nomeCartao);
                assertThat(c.getDataCadastro()).isEqualTo(dataCadastro);
                assertThat(c.getParcelas()).isSameAs(parcelas);
                assertThat(c.getCartao()).isSameAs(cartao);
            });
    }
} 