package com.kowalski.casaapi.categoria;

import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoriaServiceTest {

    private final CategoriaRepository repository = mock(CategoriaRepository.class);
    private final CategoriaService service = new CategoriaService(repository);

    @Test
    void buscaPeloId() {
        UUID id = UUID.randomUUID();
        Categorias mercado = Categorias.builder().id(id).nome("Mercado").build();
        when(repository.findById(id)).thenReturn(Optional.of(mercado));

        assertThat(service.buscar(id)).isSameAs(mercado);
    }

    @Test
    void categoriaInexistenteDevolveNaoEncontrada() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(id))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Categoria com uuid: " + id + " não encontrado!");
    }
}
