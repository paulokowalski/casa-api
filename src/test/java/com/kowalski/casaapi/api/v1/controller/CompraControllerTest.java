package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraResponse;
import com.kowalski.casaapi.domain.model.Compra;
import com.kowalski.casaapi.domain.service.CompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraControllerTest {

    @Mock
    private CompraService compraService;

    @InjectMocks
    private CompraController compraController;

    private Compra compra;
    private CompraInput compraInput;
    private CompraResponse compraResponse;

    @BeforeEach
    void setUp() {
        compra = Compra.builder()
            .id(UUID.randomUUID())
            .nomeProduto("PRODUTO TESTE")
            .valorProduto(BigDecimal.valueOf(1000))
            .dataCompra(LocalDate.now())
            .numeroParcelas(3)
            .nomePessoaCompra("JOÃO")
            .nomeCartao("NUBANK")
            .dataCadastro(LocalDateTime.now())
            .build();

        compraInput = new CompraInput(
            "Produto Teste",
            BigDecimal.valueOf(1000),
            LocalDate.now(),
            3,
            "João",
            "NUBANK"
        );

        compraResponse = new CompraResponse(List.of(), null);
    }

    @Test
    @DisplayName("Deve buscar todas as compras com sucesso")
    void buscarTodos_Sucesso() {
        // Arrange
        when(compraService.buscarTodos()).thenReturn(List.of(compra));

        // Act
        List<Compra> result = compraController.buscarTodos();

        // Assert
        assertThat(result)
            .isNotEmpty()
            .hasSize(1)
            .contains(compra);
    }

    @Test
    @DisplayName("Deve buscar compras por mês e nome com sucesso")
    void buscarPorMesENome_Sucesso() {
        // Arrange
        when(compraService.buscarPorMesENome("2024", "01", "joao", "NUBANK", "NAO"))
            .thenReturn(compraResponse);

        // Act
        CompraResponse result = compraController.buscarPorMesENome("2024", "01", "joao", "NUBANK", "NAO");

        // Assert
        assertThat(result).isNotNull();
        verify(compraService).buscarPorMesENome("2024", "01", "joao", "NUBANK", "NAO");
    }

    @Test
    @DisplayName("Deve salvar compra com sucesso")
    void salvar_Sucesso() {
        // Act
        String result = compraController.salvar(compraInput);

        // Assert
        assertThat(result).isEqualTo("Cadastro realizado com sucesso.");
        verify(compraService).salvar(compraInput);
    }

    @Test
    @DisplayName("Deve remover compra com sucesso")
    void remover_Sucesso() {
        // Arrange
        String id = UUID.randomUUID().toString();

        // Act
        compraController.remover(id);

        // Assert
        verify(compraService).remover(UUID.fromString(id));
    }

    @Test
    @DisplayName("Deve enviar evento com sucesso")
    void buscarMesAtual_Sucesso() {
        // Act
        compraController.buscarMesAtual();

        // Assert
        verify(compraService).enviarEvento();
    }
} 