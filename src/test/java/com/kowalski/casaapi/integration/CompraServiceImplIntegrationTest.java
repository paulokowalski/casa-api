package com.kowalski.casaapi.integration;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.domain.model.Cartao;
import com.kowalski.casaapi.domain.repository.CartaoRepository;
import com.kowalski.casaapi.domain.repository.CompraParcelaRepository;
import com.kowalski.casaapi.domain.repository.CompraRepository;
import com.kowalski.casaapi.domain.service.CompraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CompraServiceImplIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CompraService compraService;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private CompraParcelaRepository compraParcelaRepository;

    @Autowired
    private CartaoRepository cartaoRepository;

    private CompraInput compraInput;
    private Cartao cartao;

    @BeforeEach
    void setUp() {
        compraParcelaRepository.deleteAll();
        compraRepository.deleteAll();
        cartaoRepository.deleteAll();

        cartao = new Cartao();
        cartao.setNome("NUBANK");
        cartao = cartaoRepository.save(cartao);

        compraInput = new CompraInput(
            "Produto Teste",
            BigDecimal.valueOf(1000),
            LocalDate.now(),
            3,
            "JOAO",
            "NUBANK"
        );
    }

    @Test
    @DisplayName("Deve salvar compra com sucesso - Teste de Integração")
    void salvar_Sucesso() {
        // Act
        compraService.salvar(compraInput);

        // Assert
        assertThat(compraRepository.findAll())
            .hasSize(1)
            .first()
            .satisfies(compra -> {
                assertThat(compra.getNomeProduto()).isEqualTo(compraInput.nomeProduto());
                assertThat(compra.getValorProduto()).isEqualByComparingTo(compraInput.valorProduto());
                assertThat(compra.getDataCompra()).isEqualTo(compraInput.dataCompra());
                assertThat(compra.getNumeroParcelas()).isEqualTo(compraInput.numeroParcelas());
                assertThat(compra.getNomePessoaCompra()).isEqualTo(compraInput.nomePessoaCompra());
                assertThat(compra.getNomeCartao()).isEqualTo(compraInput.nomeCartao());
                assertThat(compra.getCartao().getId()).isEqualTo(cartao.getId());
            });

        assertThat(compraParcelaRepository.findAll())
            .hasSize(3)
            .allSatisfy(parcela -> {
                assertThat(parcela.getCompra()).isNotNull();
                assertThat(parcela.getValorParcela()).isNotNull();
                assertThat(parcela.getDataParcela()).isNotNull();
            });
    }
} 