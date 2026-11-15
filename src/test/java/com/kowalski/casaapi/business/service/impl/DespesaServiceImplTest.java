package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.response.DespesaAnualResponse;
import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import com.kowalski.casaapi.business.dao.DespesaDao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DespesaServiceImplTest {

    private final DespesaDao dao = mock(DespesaDao.class);
    private final DespesaServiceImpl service = new DespesaServiceImpl(dao);

    private static DespesaResponse despesa(String valor) {
        return new DespesaResponse(new BigDecimal(valor), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Test
    void anualJuntaSoOsMesesComDespesaEmOrdem() {
        when(dao.findByNomePessoaAndMesAnoReferencia(eq("ANA"), anyString(), eq("2026"))).thenReturn(null);
        when(dao.findByNomePessoaAndMesAnoReferencia("ANA", "10", "2026")).thenReturn(despesa("10"));
        when(dao.findByNomePessoaAndMesAnoReferencia("ANA", "02", "2026")).thenReturn(despesa("2"));

        DespesaAnualResponse r = service.buscarDespesasAnuais("2026", "ana");

        assertThat(r.pessoa()).isEqualTo("ANA");
        assertThat(r.ano()).isEqualTo("2026");
        assertThat(r.despesasPorMes()).containsOnlyKeys("02", "10");
        assertThat(r.despesasPorMes().keySet()).containsExactly("02", "10");
        verify(dao, times(12)).findByNomePessoaAndMesAnoReferencia(eq("ANA"), anyString(), eq("2026"));
    }

    @Test
    void validaOsParametros() {
        assertThatThrownBy(() -> service.buscarDespesasAnuais("", "ANA")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscarDespesasAnuais("2026", " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
