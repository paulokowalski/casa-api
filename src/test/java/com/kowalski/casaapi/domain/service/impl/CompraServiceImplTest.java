package com.kowalski.casaapi.domain.service.impl;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.domain.model.Cartao;
import com.kowalski.casaapi.domain.model.Compra;
import com.kowalski.casaapi.domain.model.CompraParcela;
import com.kowalski.casaapi.domain.repository.CompraParcelaRepository;
import com.kowalski.casaapi.domain.repository.CompraRepository;
import com.kowalski.casaapi.domain.service.CartaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompraServiceImplTest {

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private CartaoService cartaoService;

    @Mock
    private CompraParcelaRepository compraParcelaRepository;

    @Mock
    private KafkaTemplate<String, Serializable> kafkaTemplate;

    @InjectMocks
    private CompraServiceImpl compraService;

    private Compra compra;
    private CompraInput compraInput;
    private Cartao cartao;
    private List<CompraParcela> parcelas;

    @BeforeEach
    void setUp() {
        cartao = new Cartao();
        cartao.setId(UUID.randomUUID());
        cartao.setNome("NUBANK");

        compraInput = new CompraInput(
            "Produto Teste",
            BigDecimal.valueOf(1000),
            LocalDate.now(),
            3,
            "JOAO",
            "NUBANK"
        );

        compra = Compra.builder()
            .id(UUID.randomUUID())
            .nomeProduto("PRODUTO TESTE")
            .valorProduto(BigDecimal.valueOf(1000))
            .dataCompra(LocalDate.now())
            .numeroParcelas(3)
            .nomePessoaCompra("JOAO")
            .nomeCartao("NUBANK")
            .dataCadastro(LocalDateTime.now())
            .cartao(cartao)
            .parcelas(new ArrayList<>())
            .build();

        parcelas = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            parcelas.add(CompraParcela.builder()
                .id(UUID.randomUUID())
                .compra(compra)
                .numeroParcela(i)
                .valorParcela(BigDecimal.valueOf(333.33))
                .dataParcela(LocalDate.now().plusMonths(i))
                .build());
        }
    }

    @Test
    @DisplayName("Deve buscar todas as compras com sucesso")
    void buscarTodos_Sucesso() {
        // Arrange
        when(compraRepository.findAll()).thenReturn(List.of(compra));

        // Act
        List<Compra> result = compraService.buscarTodos();

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
        when(compraParcelaRepository.buscarPorMesENomeEPessoa("2024", "01", "JOAO"))
            .thenReturn(parcelas);

        // Act
        compraService.buscarPorMesENome("2024", "01", "joao", "TODOS", null);

        // Assert
        verify(compraParcelaRepository).buscarPorMesENomeEPessoa("2024", "01", "JOAO");
    }

    @Test
    @DisplayName("Deve salvar compra com sucesso")
    void salvar_Sucesso() {
        // Arrange
        when(cartaoService.findByNome("NUBANK")).thenReturn(cartao);
        when(compraRepository.save(any(Compra.class))).thenReturn(compra);

        // Act
        compraService.salvar(compraInput);

        // Assert
        verify(cartaoService).findByNome("NUBANK");
        verify(compraRepository).save(any(Compra.class));
        verify(kafkaTemplate).send(eq("compra-realizada-mensal-ha-topic"), any());
    }

    @Test
    @DisplayName("Deve remover compra com sucesso")
    void remover_Sucesso() {
        // Arrange
        when(compraRepository.findById(any(UUID.class))).thenReturn(Optional.of(compra));

        // Act
        compraService.remover(UUID.randomUUID());

        // Assert
        verify(compraRepository).delete(compra);
        verify(kafkaTemplate).send(eq("compra-realizada-mensal-ha-topic"), any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar salvar compra sem dados obrigatórios")
    void salvar_DadosObrigatoriosAusentes() {
        // Arrange
        CompraInput compraInvalida = new CompraInput(
            "",
            null,
            null,
            0,
            "",
            ""
        );

        // Act & Assert
        assertThatThrownBy(() -> compraService.salvar(compraInvalida))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("O nome do produto é obrigatório");
    }

    @Test
    @DisplayName("Deve calcular valor das parcelas corretamente")
    void salvar_CalculoParcelasCorreto() {
        // Arrange
        when(cartaoService.findByNome("NUBANK")).thenReturn(cartao);
        when(compraRepository.save(any(Compra.class))).thenReturn(compra);

        // Act
        compraService.salvar(compraInput);

        // Assert
        verify(compraRepository).save(argThat(compra -> {
            List<CompraParcela> parcelas = compra.getParcelas();
            return parcelas.size() == 3 &&
                   parcelas.stream().allMatch(p -> p.getValorParcela().compareTo(BigDecimal.valueOf(333.33)) == 0);
        }));
    }
} 