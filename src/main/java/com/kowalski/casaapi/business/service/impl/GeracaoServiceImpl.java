package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoMesAnoResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoMensalResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoDiariaResponse;
import com.kowalski.casaapi.business.model.Geracao;
import com.kowalski.casaapi.business.model.GeracaoDiaria;
import com.kowalski.casaapi.business.repository.GeracaoRepository;
import com.kowalski.casaapi.business.service.GeracaoDiariaService;
import com.kowalski.casaapi.business.service.GeracaoService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeracaoServiceImpl implements GeracaoService {

    private final GeracaoRepository geracaoRepository;
    private final GeracaoDiariaService geracaoDiariaService;

    @Override
    public GeracaoDiariaResponse buscarGeracao(LocalDate data) {
        var geracoes = geracaoRepository.buscarPorData(data).stream().map(ger -> new GeracaoResponse(ger.getPotencia(), ger.getData())).toList();
        var gerado = geracaoRepository.buscarUltimaGeracao(data);
        return new GeracaoDiariaResponse(geracoes, gerado);
    }

    @Override
    public GeracaoMensalResponse buscarGeracaoMensal(int ano, int mes) {
        var geracoes = geracaoDiariaService.buscarPorAnoMes(mes, ano).stream().map(ger -> new GeracaoResponse(ger.getGeracao(), LocalDateTime.of(ger.getData(), LocalTime.now()))).toList();
        return new GeracaoMensalResponse(geracoes, Year.of(ano), Month.of(mes));
    }

    @Override
    public List<GeracaoMesAnoResponse> buscarGeracaoAnual(int year) {

        var geracoes = geracaoDiariaService.buscarPorAno(year);
        Map<Month, List<GeracaoDiaria>> map = geracoes.stream().collect(Collectors.groupingBy(g -> Month.from(g.getData())));
        Map<Month, Double> totalPorMes = map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .mapToDouble(GeracaoDiaria::getGeracao)
                                .sum()
                ));

        List<GeracaoMesAnoResponse> lista = totalPorMes.entrySet().stream()
                .map(e -> new GeracaoMesAnoResponse(e.getKey(), Year.of(year), e.getValue()))
                .collect(Collectors.toList());

        return lista;
    }

    @Override
    @Transactional
    public void salvar(GeradoDTO dto) {
        var geracao = Geracao.builder().data(LocalDateTime.now()).geracao(dto.getGerado()).potencia(dto.getPotencia()).build();
        geracaoRepository.save(geracao);
        salvarGeracaoDiaria(geracao);
    }

    public void salvarGeracaoDiaria(Geracao geracao){
        var geracaoDiaria = GeracaoDiaria.builder().geracao(geracao.getGeracao()).data(geracao.getData().toLocalDate()).build();
        geracaoDiariaService.salvar(geracaoDiaria);
    }

}