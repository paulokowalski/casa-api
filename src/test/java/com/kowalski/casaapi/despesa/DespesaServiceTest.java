package com.kowalski.casaapi.despesa;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DespesaServiceTest {

    private final DespesaRepository repository = mock(DespesaRepository.class);
    private final DespesaService service = new DespesaService(repository);

    private static DespesaResponse despesa(String valor) {
        return new DespesaResponse(new BigDecimal(valor), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Test
    void anualJuntaSoOsMesesComDespesaEmOrdem() {
        when(repository.totaisDoMes("ANA", LocalDate.of(2026, 10, 1))).thenReturn(despesa("10"));
        when(repository.totaisDoMes("ANA", LocalDate.of(2026, 2, 1))).thenReturn(despesa("2"));

        DespesaAnualResponse r = service.buscarDespesasAnuais("2026", "ana");

        assertThat(r.pessoa()).isEqualTo("ANA");
        assertThat(r.ano()).isEqualTo("2026");
        assertThat(r.despesasPorMes().keySet()).containsExactly("02", "10");
        verify(repository, times(12)).totaisDoMes(eq("ANA"), any());
    }

    @Test
    void validaOsParametros() {
        assertThatThrownBy(() -> service.buscarDespesasAnuais("", "ANA")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscarDespesasAnuais("2026", " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
