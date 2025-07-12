package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.business.model.GeracaoDiaria;
import com.kowalski.casaapi.business.repository.GeracaoDiariaRepository;
import com.kowalski.casaapi.business.service.GeracaoDiariaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeracaoDiariaServiceImpl implements GeracaoDiariaService {

    private final GeracaoDiariaRepository geracaoDiariaRepository;

    @Override
    public GeracaoDiaria salvar(GeracaoDiaria geracaoDiaria) {
        return geracaoDiariaRepository.findByData(geracaoDiaria.getData()).map(geracao -> {
            geracao.setGeracao(geracaoDiaria.getGeracao());
            return geracaoDiariaRepository.save(geracao);
        }).orElseGet(() -> geracaoDiariaRepository.save(geracaoDiaria));
    }

    @Override
    public List<GeracaoDiaria> buscarPorAnoMes(int ano, int mes) {
        return geracaoDiariaRepository.buscarPorAnoMes(mes, ano);
    }

    @Override
    public List<GeracaoDiaria> buscarPorAno(int ano) {
        return geracaoDiariaRepository.buscarPorAno(ano);
    }
}
