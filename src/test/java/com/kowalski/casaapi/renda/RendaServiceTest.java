package com.kowalski.casaapi.renda;

import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.pessoa.PessoaService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RendaServiceTest {

    private static final LocalDate NOV = LocalDate.of(2026, 11, 1);

    private final PessoaService pessoaService = mock(PessoaService.class);
    private final RendaMensalRepository rendaRepository = mock(RendaMensalRepository.class);
    private final RendaService renda = new RendaService(rendaRepository, pessoaService);

    @Test
    void rendaDoProprioMesNaoEHerdada() {
        when(rendaRepository.findFirstByIdPessoaIdAndIdCompetenciaLessThanEqualOrderByIdCompetenciaDesc(1L, NOV))
                .thenReturn(Optional.of(new RendaMensal(new RendaMensal.Id(1L, NOV), new BigDecimal("5000.00"))));

        assertThat(renda.obter(1L, NOV))
                .isEqualTo(new RendaResponse("2026-11", new BigDecimal("5000.00"), "2026-11", false));
    }

    @Test
    void mesSemRendaHerdaAMaisRecente() {
        var setembro = LocalDate.of(2026, 9, 1);
        when(rendaRepository.findFirstByIdPessoaIdAndIdCompetenciaLessThanEqualOrderByIdCompetenciaDesc(1L, NOV))
                .thenReturn(Optional.of(new RendaMensal(new RendaMensal.Id(1L, setembro), new BigDecimal("4000.00"))));

        assertThat(renda.obter(1L, NOV))
                .isEqualTo(new RendaResponse("2026-11", new BigDecimal("4000.00"), "2026-09", true));
    }

    @Test
    void semNenhumaRendaVoltaVazio() {
        assertThat(renda.obter(1L, NOV)).isEqualTo(new RendaResponse("2026-11", null, null, false));
    }

    @Test
    void valorZeradoOuNuloRemoveARendaDoMes() {
        renda.salvar(1L, NOV, new RendaInput(BigDecimal.ZERO));
        renda.salvar(1L, NOV, new RendaInput(null));
        renda.salvar(1L, NOV, null);

        verify(rendaRepository, times(3)).deleteById(new RendaMensal.Id(1L, NOV));
        verify(rendaRepository, never()).save(any());
    }

    @Test
    void valorPositivoSalvaARenda() {
        renda.salvar(1L, NOV, new RendaInput(new BigDecimal("10.00")));

        verify(rendaRepository).save(argThat(r -> r.getValor().compareTo(BigDecimal.TEN) == 0
                && r.getId().equals(new RendaMensal.Id(1L, NOV))));
    }

    @Test
    void rendaDePessoaInexistenteDa404() {
        doThrow(new RecursoNaoEncontradoException("Pessoa 99 não encontrada")).when(pessoaService).garantirQueExiste(99L);

        assertThatThrownBy(() -> renda.salvar(99L, NOV, new RendaInput(BigDecimal.TEN)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(rendaRepository, never()).save(any());
    }
}
