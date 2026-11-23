package com.kowalski.casaapi.passagens;

import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.comum.RelogioConfig;
import com.kowalski.casaapi.passagens.RotaPassagemInput.Data;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RotasPassagemServiceTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), RelogioConfig.FUSO_DA_CASA);

    private final RotaPassagemRepository repository = mock(RotaPassagemRepository.class);
    private final RotasPassagemService service = new RotasPassagemService(repository, RELOGIO);

    private static RotaPassagemInput rota(String nome) {
        return new RotaPassagemInput(nome, null, List.of("FOR"), List.of("GRU"),
                List.of(new Data(LocalDate.of(2027, 9, 1), null)), null, 1000, 1, null, null, null, null, null);
    }

    @Test
    void nomeRepetidoNaoGrava() {
        when(repository.existe("Viagem", null)).thenReturn(true);

        assertThatThrownBy(() -> service.criar(rota("Viagem")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Já existe uma rota chamada \"Viagem\"");
        verify(repository, never()).inserir(any());
    }

    @Test
    void renomearLevaOHistoricoJunto() {
        when(repository.nome(7L)).thenReturn(Optional.of("Antiga"));
        when(repository.buscar(7L)).thenReturn(Map.of());

        service.atualizar(7L, rota("Nova"));

        verify(repository).atualizar(eq(7L), any());
        verify(repository).renomearHistorico("Antiga", "Nova");
    }

    @Test
    void manterONomeNaoMexeNoHistorico() {
        when(repository.nome(7L)).thenReturn(Optional.of("Viagem"));
        when(repository.buscar(7L)).thenReturn(Map.of());

        service.atualizar(7L, rota("Viagem"));

        verify(repository, never()).renomearHistorico(any(), any());
    }

    @Test
    void removerSoApagaOHistoricoQuandoPedido() {
        when(repository.nome(7L)).thenReturn(Optional.of("Viagem"));

        service.remover(7L, false);
        verify(repository, never()).apagarHistorico(any());

        service.remover(7L, true);
        verify(repository).apagarHistorico("Viagem");
        verify(repository, times(2)).remover(7L);
    }

    @Test
    void rotaInexistenteDa404() {
        when(repository.nome(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remover(99L, true))
                .isInstanceOf(RecursoNaoEncontradoException.class).hasMessage("Rota 99 não encontrada");
    }
}
