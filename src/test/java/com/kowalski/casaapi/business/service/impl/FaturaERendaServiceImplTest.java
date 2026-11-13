package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.input.FaturaInput;
import com.kowalski.casaapi.api.v1.input.RendaInput;
import com.kowalski.casaapi.api.v1.response.FaturaResponse;
import com.kowalski.casaapi.api.v1.response.RendaResponse;
import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.model.FaturaPaga;
import com.kowalski.casaapi.business.model.RendaMensal;
import com.kowalski.casaapi.business.repository.FaturaPagaRepository;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import com.kowalski.casaapi.business.repository.RendaMensalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FaturaERendaServiceImplTest {

    private static final LocalDate NOV = LocalDate.of(2026, 11, 1);

    private final PessoaRepository pessoaRepository = mock(PessoaRepository.class);
    private final FaturaPagaRepository faturaRepository = mock(FaturaPagaRepository.class);
    private final RendaMensalRepository rendaRepository = mock(RendaMensalRepository.class);
    private final FaturaServiceImpl fatura = new FaturaServiceImpl(faturaRepository, pessoaRepository);
    private final RendaServiceImpl renda = new RendaServiceImpl(rendaRepository, pessoaRepository);

    @BeforeEach
    void setUp() {
        when(pessoaRepository.existsById(1L)).thenReturn(true);
    }


    @Test
    void faturaSemRegistroNaoEstaPaga() {
        assertThat(fatura.obter(1L, NOV)).isEqualTo(new FaturaResponse("2026-11", false, null));
    }

    @Test
    void marcarComoPagaSemDataUsaHoje() {
        var id = new FaturaPaga.Id(1L, NOV);
        when(faturaRepository.findById(id)).thenReturn(Optional.of(new FaturaPaga(id, LocalDate.now())));

        FaturaResponse r = fatura.salvar(1L, NOV, new FaturaInput(true, null));

        verify(faturaRepository).save(argThat(f -> f.getDataPagamento().equals(LocalDate.now())));
        assertThat(r.paga()).isTrue();
    }

    @Test
    void desmarcarApagaORegistro() {
        fatura.salvar(1L, NOV, new FaturaInput(false, null));

        verify(faturaRepository).deleteById(new FaturaPaga.Id(1L, NOV));
        verify(faturaRepository, never()).save(any());
    }

    @Test
    void faturaDePessoaInexistenteDa404() {
        assertThatThrownBy(() -> fatura.salvar(99L, NOV, new FaturaInput(true, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }


    @Test
    void rendaDoProprioMesNaoEHerdada() {
        when(rendaRepository.findFirstByIdPessoaIdAndIdCompetenciaLessThanEqualOrderByIdCompetenciaDesc(1L, NOV))
                .thenReturn(Optional.of(new RendaMensal(new RendaMensal.Id(1L, NOV), new BigDecimal("5000.00"))));

        assertThat(renda.obter(1L, NOV))
                .isEqualTo(new RendaResponse("2026-11", new BigDecimal("5000.00"), "2026-11", false));
    }

    @Test
    void mesSemRendaHerdaAMaisRecente() {
        var set = LocalDate.of(2026, 9, 1);
        when(rendaRepository.findFirstByIdPessoaIdAndIdCompetenciaLessThanEqualOrderByIdCompetenciaDesc(1L, NOV))
                .thenReturn(Optional.of(new RendaMensal(new RendaMensal.Id(1L, set), new BigDecimal("4000.00"))));

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
        assertThatThrownBy(() -> renda.salvar(99L, NOV, new RendaInput(BigDecimal.TEN)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
