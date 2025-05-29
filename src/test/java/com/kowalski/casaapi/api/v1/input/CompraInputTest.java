package com.kowalski.casaapi.api.v1.input;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CompraInputTest {

    @Test
    @DisplayName("Deve criar CompraInput com sucesso")
    void criarCompraInput_Sucesso() {
        // Arrange
        String nomeProduto = "Produto Teste";
        BigDecimal valorProduto = BigDecimal.valueOf(1000);
        LocalDate dataCompra = LocalDate.now();
        Integer numeroParcelas = 3;
        String nomePessoaCompra = "JOAO";
        String nomeCartao = "NUBANK";

        // Act
        CompraInput input = new CompraInput(
            nomeProduto,
            valorProduto,
            dataCompra,
            numeroParcelas,
            nomePessoaCompra,
            nomeCartao
        );

        // Assert
        assertThat(input)
            .isNotNull()
            .satisfies(i -> {
                assertThat(i.nomeProduto()).isEqualTo(nomeProduto);
                assertThat(i.valorProduto()).isEqualByComparingTo(valorProduto);
                assertThat(i.dataCompra()).isEqualTo(dataCompra);
                assertThat(i.numeroParcelas()).isEqualTo(numeroParcelas);
                assertThat(i.nomePessoaCompra()).isEqualTo(nomePessoaCompra);
                assertThat(i.nomeCartao()).isEqualTo(nomeCartao);
            });
    }

    @Test
    @DisplayName("Deve aceitar valores nulos")
    void criarCompraInput_ValoresNulos() {
        // Act
        CompraInput input = new CompraInput(null, null, null, null, null, null);

        // Assert
        assertThat(input)
            .isNotNull()
            .satisfies(i -> {
                assertThat(i.nomeProduto()).isNull();
                assertThat(i.valorProduto()).isNull();
                assertThat(i.dataCompra()).isNull();
                assertThat(i.numeroParcelas()).isNull();
                assertThat(i.nomePessoaCompra()).isNull();
                assertThat(i.nomeCartao()).isNull();
            });
    }

    @Test
    @DisplayName("Deve gerar toString corretamente")
    void toString_Sucesso() {
        // Arrange
        CompraInput input = new CompraInput(
            "Produto",
            BigDecimal.TEN,
            LocalDate.of(2024, 1, 1),
            3,
            "JOAO",
            "NUBANK"
        );

        // Act
        String result = input.toString();

        // Assert
        assertThat(result)
            .contains("nomeProduto=Produto")
            .contains("valorProduto=10")
            .contains("dataCompra=2024-01-01")
            .contains("numeroParcelas=3")
            .contains("nomePessoaCompra=JOAO")
            .contains("nomeCartao=NUBANK");
    }
} 