package com.kowalski.casaapi.geracao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;

@Service
@RequiredArgsConstructor
public class GeracaoService {

    private final GeracaoRepository geracaoRepository;
    private final GeracaoDiariaRepository diariaRepository;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public GeracaoDiariaResponse doDia(LocalDate dia) {
        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fim = dia.plusDays(1).atStartOfDay();
        var leituras = geracaoRepository.findByDataGreaterThanEqualAndDataLessThanOrderByDataAsc(inicio, fim).stream()
                .map(g -> new GeracaoResponse(g.getPotencia(), g.getData()))
                .toList();
        return new GeracaoDiariaResponse(leituras, geracaoRepository.maiorGeracaoEntre(inicio, fim));
    }

    @Transactional(readOnly = true)
    public GeracaoMensalResponse doMes(int ano, int mes) {
        YearMonth competencia = YearMonth.of(ano, mes);
        LocalTime agora = LocalTime.now(relogio);
        var dias = diariaRepository.findByDataBetweenOrderByDataAsc(competencia.atDay(1), competencia.atEndOfMonth()).stream()
                .map(d -> new GeracaoResponse(d.getGeracao(), LocalDateTime.of(d.getData(), agora)))
                .toList();
        return new GeracaoMensalResponse(dias, Year.of(ano), Month.of(mes));
    }

    @Transactional(readOnly = true)
    public List<GeracaoMesAnoResponse> doAno(int ano) {
        Year year = Year.of(ano);
        Map<Month, Double> totalPorMes = diariaRepository.findByDataBetweenOrderByDataAsc(year.atDay(1), year.atMonth(12).atEndOfMonth())
                .stream()
                .collect(groupingBy(d -> d.getData().getMonth(), TreeMap::new, summingDouble(GeracaoDiaria::getGeracao)));
        return totalPorMes.entrySet().stream()
                .map(e -> new GeracaoMesAnoResponse(e.getKey(), year, e.getValue()))
                .toList();
    }

    @Transactional
    public void registrar(GeradoDTO leitura) {
        Geracao geracao = geracaoRepository.save(Geracao.registrar(leitura, LocalDateTime.now(relogio)));
        diariaRepository.findByData(geracao.getData().toLocalDate())
                .ifPresentOrElse(dia -> dia.atualizarTotal(geracao), () -> diariaRepository.save(GeracaoDiaria.doDia(geracao)));
    }
}
