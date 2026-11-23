package com.kowalski.casaapi.fatura;

import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.comum.RelogioConfig;
import com.kowalski.casaapi.pessoa.PessoaService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FaturaServiceTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), RelogioConfig.FUSO_DA_CASA);
    private static final LocalDate NOV = LocalDate.of(2026, 11, 1);

    private final PessoaService pessoaService = mock(PessoaService.class);
    private final FaturaPagaRepository faturaRepository = mock(FaturaPagaRepository.class);
    private final FaturaService fatura = new FaturaService(faturaRepository, pessoaService, RELOGIO);

    @Test
    void faturaSemRegistroNaoEstaPaga() {
        assertThat(fatura.obter(1L, NOV)).isEqualTo(new FaturaResponse("2026-11", false, null));
    }

    @Test
    void marcarComoPagaSemDataUsaHoje() {
        var id = new FaturaPaga.Id(1L, NOV);
        when(faturaRepository.findById(id)).thenReturn(Optional.of(new FaturaPaga(id, LocalDate.now(RELOGIO))));

        FaturaResponse r = fatura.salvar(1L, NOV, new FaturaInput(true, null));

        verify(faturaRepository).save(argThat(f -> f.getDataPagamento().equals(LocalDate.now(RELOGIO))));
        assertThat(r).isEqualTo(new FaturaResponse("2026-11", true, LocalDate.of(2026, 11, 23)));
    }

    @Test
    void desmarcarApagaORegistro() {
        fatura.salvar(1L, NOV, new FaturaInput(false, null));

        verify(faturaRepository).deleteById(new FaturaPaga.Id(1L, NOV));
        verify(faturaRepository, never()).save(any());
    }

    @Test
    void faturaDePessoaInexistenteDa404() {
        doThrow(new RecursoNaoEncontradoException("Pessoa 99 não encontrada")).when(pessoaService).garantirQueExiste(99L);

        assertThatThrownBy(() -> fatura.salvar(99L, NOV, new FaturaInput(true, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(faturaRepository, never()).save(any());
    }
}
