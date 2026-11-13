package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoMesAnoResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoMensalResponse;
import com.kowalski.casaapi.business.model.Geracao;
import com.kowalski.casaapi.business.model.GeracaoDiaria;
import com.kowalski.casaapi.business.repository.GeracaoDiariaRepository;
import com.kowalski.casaapi.business.repository.GeracaoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeracaoServiceImplTest {

    private final GeracaoRepository geracaoRepository = mock(GeracaoRepository.class);
    private final GeracaoDiariaRepository diariaRepository = mock(GeracaoDiariaRepository.class);
    private final GeracaoDiariaServiceImpl diaria = new GeracaoDiariaServiceImpl(diariaRepository);
    private final GeracaoServiceImpl service = new GeracaoServiceImpl(geracaoRepository, diaria);

    private static GeracaoDiaria dia(int ano, int mes, int d, double valor) {
        return GeracaoDiaria.builder().data(LocalDate.of(ano, mes, d)).geracao(valor).build();
    }

    @Test
    void mensalConsultaOAnoEOMesCertos() {
        when(diariaRepository.buscarPorAnoMes(2025, 3)).thenReturn(List.of(dia(2025, 3, 1, 10.5), dia(2025, 3, 2, 12.0)));

        GeracaoMensalResponse r = service.buscarGeracaoMensal(2025, 3);

        verify(diariaRepository).buscarPorAnoMes(2025, 3);
        assertThat(r.ano()).isEqualTo(Year.of(2025));
        assertThat(r.mes()).isEqualTo(Month.MARCH);
        assertThat(r.valores()).extracting(v -> v.valor(), v -> v.data().toLocalDate())
                .containsExactly(tuple(10.5, LocalDate.of(2025, 3, 1)), tuple(12.0, LocalDate.of(2025, 3, 2)));
    }

    @Test
    void anualSomaPorMes() {
        when(diariaRepository.buscarPorAno(2025)).thenReturn(List.of(
                dia(2025, 1, 1, 1.0), dia(2025, 1, 2, 2.5), dia(2025, 2, 1, 4.0)));

        List<GeracaoMesAnoResponse> r = service.buscarGeracaoAnual(2025);

        assertThat(r).extracting(GeracaoMesAnoResponse::month, GeracaoMesAnoResponse::year, GeracaoMesAnoResponse::value)
                .containsExactlyInAnyOrder(tuple(Month.JANUARY, Year.of(2025), 3.5), tuple(Month.FEBRUARY, Year.of(2025), 4.0));
    }

    @Test
    void salvarGuardaALeituraEAtualizaOTotalDoDia() {
        GeracaoDiaria existente = dia(2025, 1, 1, 1.0);
        existente.setData(LocalDate.now());
        when(diariaRepository.findByData(LocalDate.now())).thenReturn(Optional.of(existente));
        GeradoDTO dto = new GeradoDTO();
        dto.setGerado(7.5);
        dto.setPotencia(1200.0);

        service.salvar(dto);

        verify(geracaoRepository).save(argThat((Geracao g) -> g.getGeracao() == 7.5 && g.getPotencia() == 1200.0));
        assertThat(existente.getGeracao()).isEqualTo(7.5);
        verify(diariaRepository).save(existente);
    }

    @Test
    void primeiraLeituraDoDiaCriaOTotal() {
        when(diariaRepository.findByData(any())).thenReturn(Optional.empty());
        GeradoDTO dto = new GeradoDTO();
        dto.setGerado(1.0);
        dto.setPotencia(10.0);

        service.salvar(dto);

        verify(diariaRepository).save(argThat(d -> d.getId() == null && d.getData().equals(LocalDate.now())));
    }
}
