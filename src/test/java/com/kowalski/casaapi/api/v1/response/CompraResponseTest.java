package com.kowalski.casaapi.api.v1.response;

import com.kowalski.casaapi.domain.model.Compra;
import com.kowalski.casaapi.domain.model.CompraParcela;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompraResponseTest {

    private Compra compra;
    private CompraParcela parcela;
    private CompraResponse compraResponse;

    @BeforeEach
    void setUp() {
        compra = Compra.builder()
            .id(UUID.randomUUID())
            .nomeProduto("PRODUTO TESTE")
            .valorProduto(BigDecimal.valueOf(1000))
            .dataCompra(LocalDate.now())
            .numeroParcelas(3)
            .nomePessoaCompra("JOAO")
            .nomeCartao("NUBANK")
            .dataCadastro(LocalDateTime.now())
            .build();

        parcela = CompraParcela.builder()
            .id(UUID.randomUUID())
            .compra(compra)
            .numeroParcela(1)
            .valorParcela(BigDecimal.valueOf(333.33))
            .dataParcela(LocalDate.now())
            .build();

        compraResponse = new CompraResponse(null, null);
    }

    @Test
    @DisplayName("Deve converter CompraParcela para CompraResponse com sucesso")
    void to_Sucesso() {
        // Act
        CompraResponse result = compraResponse.to(List.of(parcela), null);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.compras())
            .isNotNull()
            .hasSize(1)
            .first()
            .satisfies(response -> {
                assertThat(response.id()).isEqualTo(compra.getId().toString());
                assertThat(response.nomeCartao()).isEqualTo("NUBANK");
                assertThat(response.nomePessoa()).isEqualTo("JOAO");
            });
    }

    @Test
    @DisplayName("Deve filtrar por última parcela quando informado")
    void to_FiltrarUltimaParcela() {
        // Arrange
        parcela.setNumeroParcela(3); // Última parcela

        // Act
        CompraResponse result = compraResponse.to(List.of(parcela), "SIM");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.compras()).hasSize(1);
    }

    @Test
    @DisplayName("Deve montar gráfico corretamente")
    void to_MontarGrafico() {
        // Act
        CompraResponse result = compraResponse.to(List.of(parcela), null);

        // Assert
        assertThat(result.data())
            .isNotNull()
            .containsKey("labels")
            .containsKey("datasets");

        Map<String, Object> data = result.data();
        assertThat(data.get("labels"))
            .asList()
            .hasSize(1)
            .contains("NUBANK");

        Object[] datasets = (Object[]) data.get("datasets");
        assertThat(datasets).hasSize(1);
    }

    @Test
    @DisplayName("Deve calcular valor faltante corretamente")
    void to_CalcularValorFaltante() {
        // Act
        CompraResponse result = compraResponse.to(List.of(parcela), null);

        // Assert
        assertThat(result.compras())
            .isNotNull()
            .hasSize(1);
    }

    @Test
    @DisplayName("Deve criar CompraResponse vazio")
    void criarCompraResponse_Vazio() {
        // Act
        CompraResponse response = new CompraResponse(null, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.compras()).isNull();
        assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("Deve gerar toString corretamente")
    void toString_Sucesso() {
        // Arrange
        CompraResponse response = new CompraResponse(List.of(), Map.of());

        // Act
        String result = response.toString();

        // Assert
        assertThat(result)
            .contains("compras=[]")
            .contains("data={}");
    }

    @Test
    @DisplayName("Deve comparar objetos corretamente")
    void equals_Sucesso() {
        // Arrange
        CompraResponse response1 = new CompraResponse(List.of(), Map.of());
        CompraResponse response2 = new CompraResponse(List.of(), Map.of());
        CompraResponse response3 = new CompraResponse(null, null);

        // Assert
        assertThat(response1)
            .isEqualTo(response2)
            .isNotEqualTo(response3);
    }

    @Test
    @DisplayName("Deve gerar hashCode corretamente")
    void hashCode_Sucesso() {
        // Arrange
        CompraResponse response1 = new CompraResponse(List.of(), Map.of());
        CompraResponse response2 = new CompraResponse(List.of(), Map.of());

        // Assert
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }
} 