package com.kowalski.casaapi.domain.service.impl;

import com.kowalski.casaapi.domain.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.domain.model.Cartao;
import com.kowalski.casaapi.domain.repository.CartaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartaoServiceImplTest {

    @Mock
    private CartaoRepository repository;

    @InjectMocks
    private CartaoServiceImpl cartaoService;

    private Cartao cartao;

    @BeforeEach
    void setUp() {
        cartao = new Cartao();
        cartao.setId(UUID.randomUUID());
        cartao.setNome("NUBANK");
    }

    @Test
    @DisplayName("Deve encontrar cartão por nome com sucesso")
    void findByNome_Sucesso() {
        // Arrange
        when(repository.findByNome("NUBANK")).thenReturn(Optional.of(cartao));

        // Act
        Cartao result = cartaoService.findByNome("NUBANK");

        // Assert
        assertThat(result)
            .isNotNull()
            .satisfies(c -> {
                assertThat(c.getId()).isEqualTo(cartao.getId());
                assertThat(c.getNome()).isEqualTo("NUBANK");
            });
    }

    @Test
    @DisplayName("Deve lançar exceção quando cartão não for encontrado")
    void findByNome_CartaoNaoEncontrado() {
        // Arrange
        when(repository.findByNome("CARTAO_INEXISTENTE")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> cartaoService.findByNome("CARTAO_INEXISTENTE"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Cartão com nome: CARTAO_INEXISTENTE não encontrado!");
    }
} 