package com.kowalski.casaapi.integration;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraResponse;
import com.kowalski.casaapi.domain.model.Cartao;
import com.kowalski.casaapi.domain.model.Compra;
import com.kowalski.casaapi.domain.repository.CartaoRepository;
import com.kowalski.casaapi.domain.repository.CompraRepository;
import com.kowalski.casaapi.domain.service.CompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class CompraServiceIntegrationTest {

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
    private CompraService compraService;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private CartaoRepository cartaoRepository;

    private Cartao cartao;
    private CompraInput compraInput;

    @BeforeEach
    void setUp() {
        compraRepository.deleteAll();
        cartaoRepository.deleteAll();

        cartao = new Cartao();
        cartao.setNome("NUBANK");
        cartaoRepository.save(cartao);

        compraInput = new CompraInput(
            "Produto Teste",
            BigDecimal.valueOf(1000),
            LocalDate.now(),
            3,
            "João",
            "NUBANK"
        );
    }

    @Test
    @DisplayName("Deve salvar e buscar compra com sucesso - Teste de Integração")
    void salvarEBuscarCompra() {
        // Act - Salvar
        compraService.salvar(compraInput);

        // Assert - Verificar se salvou
        List<Compra> compras = compraService.buscarTodos();
        assertThat(compras)
            .isNotEmpty()
            .hasSize(1)
            .element(0)
            .satisfies(compra -> {
                assertThat(compra.getNomeProduto()).isEqualTo("PRODUTO TESTE");
                assertThat(compra.getValorProduto()).isEqualByComparingTo(BigDecimal.valueOf(1000));
                assertThat(compra.getNomePessoaCompra()).isEqualTo("JOÃO");
                assertThat(compra.getNomeCartao()).isEqualTo("NUBANK");
                assertThat(compra.getNumeroParcelas()).isEqualTo(3);
                assertThat(compra.getParcelas()).hasSize(3);
            });

        // Act - Buscar por mês
        String mesAtual = String.format("%02d", LocalDate.now().plusMonths(1).getMonthValue());
        String anoAtual = String.valueOf(LocalDate.now().getYear());
        CompraResponse response = compraService.buscarPorMesENome(anoAtual, mesAtual, "joao", "TODOS", null);

        // Assert - Verificar busca
        assertThat(response).isNotNull();
        assertThat(response.compras())
            .isNotEmpty()
            .hasSize(3)
            .allSatisfy(parcela -> {
                assertThat(parcela.nomeCompra()).isEqualTo("PRODUTO TESTE");
                assertThat(parcela.valorParcela()).isEqualTo(333.33);
                assertThat(parcela.nomePessoa()).isEqualTo("JOÃO");
                assertThat(parcela.nomeCartao()).isEqualTo("NUBANK");
            });
    }

    @Test
    @DisplayName("Deve remover compra com sucesso - Teste de Integração")
    void removerCompra() {
        // Arrange
        compraService.salvar(compraInput);
        List<Compra> compras = compraService.buscarTodos();
        assertThat(compras).hasSize(1);

        // Act
        compraService.remover(compras.get(0).getId());

        // Assert
        assertThat(compraService.buscarTodos()).isEmpty();
    }
} 