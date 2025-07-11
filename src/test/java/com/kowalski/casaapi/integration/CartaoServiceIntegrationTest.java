package com.kowalski.casaapi.integration;

import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.model.Cartao;
import com.kowalski.casaapi.business.repository.CartaoRepository;
import com.kowalski.casaapi.business.service.CartaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartaoServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CartaoService cartaoService;

    @Autowired
    private CartaoRepository cartaoRepository;

    @BeforeEach
    void setUp() {
        cartaoRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve encontrar cartão por nome - Teste de Integração")
    void findByNome() {
        // Arrange
        Cartao cartao = new Cartao();
        cartao.setNome("NUBANK");
        cartaoRepository.save(cartao);

        // Act
        Cartao result = cartaoService.findByNome("NUBANK");

        // Assert
        assertThat(result)
            .isNotNull()
            .satisfies(c -> {
                assertThat(c.getId()).isNotNull();
                assertThat(c.getNome()).isEqualTo("NUBANK");
            });
    }

    @Test
    @DisplayName("Deve lançar exceção quando cartão não existir - Teste de Integração")
    void findByNome_CartaoInexistente() {
        assertThatThrownBy(() -> cartaoService.findByNome("CARTAO_INEXISTENTE"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Cartão com nome: CARTAO_INEXISTENTE não encontrado!");
    }
} 