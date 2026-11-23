package com.kowalski.casaapi.geracao;

import com.kowalski.casaapi.comum.RelogioConfig;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeracaoServiceTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), RelogioConfig.FUSO_DA_CASA);
    private static final LocalDate HOJE = LocalDate.now(RELOGIO);

    private final GeracaoRepository geracaoRepository = mock(GeracaoRepository.class);
    private final GeracaoDiariaRepository diariaRepository = mock(GeracaoDiariaRepository.class);
    private final GeracaoService service = new GeracaoService(geracaoRepository, diariaRepository, RELOGIO);

    private static GeracaoDiaria dia(int ano, int mes, int d, double valor) {
        return new GeracaoDiaria(LocalDate.of(ano, mes, d), valor);
    }

    @Test
    void diaConsultaDaMeiaNoiteAteAMeiaNoiteSeguinte() {
        LocalDateTime inicio = LocalDate.of(2025, 1, 2).atStartOfDay();
        LocalDateTime fim = LocalDate.of(2025, 1, 3).atStartOfDay();
        Geracao leitura = Geracao.registrar(new GeradoDTO(3.0, 1500.0), inicio.plusHours(10));
        when(geracaoRepository.findByDataGreaterThanEqualAndDataLessThanOrderByDataAsc(inicio, fim)).thenReturn(List.of(leitura));
        when(geracaoRepository.maiorGeracaoEntre(inicio, fim)).thenReturn(3.0);

        GeracaoDiariaResponse r = service.doDia(LocalDate.of(2025, 1, 2));

        assertThat(r.geracao()).isEqualTo(3.0);
        assertThat(r.valores()).extracting(GeracaoResponse::valor, GeracaoResponse::data)
                .containsExactly(tuple(1500.0, inicio.plusHours(10)));
    }

    @Test
    void mensalConsultaDoPrimeiroAoUltimoDiaDoMes() {
        when(diariaRepository.findByDataBetweenOrderByDataAsc(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28)))
                .thenReturn(List.of(dia(2025, 2, 1, 10.5), dia(2025, 2, 2, 12.0)));

        GeracaoMensalResponse r = service.doMes(2025, 2);

        assertThat(r.ano()).isEqualTo(Year.of(2025));
        assertThat(r.mes()).isEqualTo(Month.FEBRUARY);
        assertThat(r.valores()).extracting(v -> v.valor(), v -> v.data().toLocalDate())
                .containsExactly(tuple(10.5, LocalDate.of(2025, 2, 1)), tuple(12.0, LocalDate.of(2025, 2, 2)));
    }

    @Test
    void anualSomaPorMesEmOrdemDeJaneiroADezembro() {
        when(diariaRepository.findByDataBetweenOrderByDataAsc(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))).thenReturn(List.of(
                dia(2025, 12, 1, 9.0), dia(2025, 1, 1, 1.0), dia(2025, 1, 2, 2.5), dia(2025, 2, 1, 4.0)));

        List<GeracaoMesAnoResponse> r = service.doAno(2025);

        assertThat(r).extracting(GeracaoMesAnoResponse::month, GeracaoMesAnoResponse::year, GeracaoMesAnoResponse::value)
                .containsExactly(tuple(Month.JANUARY, Year.of(2025), 3.5), tuple(Month.FEBRUARY, Year.of(2025), 4.0),
                        tuple(Month.DECEMBER, Year.of(2025), 9.0));
    }

    @Test
    void leituraAtualizaOTotalDoDiaQueJaExiste() {
        when(geracaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        GeracaoDiaria existente = dia(HOJE.getYear(), HOJE.getMonthValue(), HOJE.getDayOfMonth(), 1.0);
        when(diariaRepository.findByData(HOJE)).thenReturn(Optional.of(existente));

        service.registrar(new GeradoDTO(7.5, 1200.0));

        verify(geracaoRepository).save(argThat(g -> g.getGeracao() == 7.5 && g.getPotencia() == 1200.0
                && g.getData().equals(LocalDateTime.now(RELOGIO))));
        assertThat(existente.getGeracao()).isEqualTo(7.5);
        verify(diariaRepository, never()).save(any());
    }

    @Test
    void primeiraLeituraDoDiaCriaOTotal() {
        when(geracaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(diariaRepository.findByData(any())).thenReturn(Optional.empty());

        service.registrar(new GeradoDTO(1.0, 10.0));

        verify(diariaRepository).save(argThat(d -> d.getId() == null && d.getData().equals(HOJE) && d.getGeracao() == 1.0));
    }
}
