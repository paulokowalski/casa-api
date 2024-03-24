package com.kowalski.casaapi.domain.service.impl;

import com.kowalski.casaapi.domain.model.GeracaoDiaria;
import com.kowalski.casaapi.domain.repository.GeracaoDiariaRepository;
import com.kowalski.casaapi.domain.service.GeracaoDiariaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class GeracaoDiariaImpl implements GeracaoDiariaService {

    private final GeracaoDiariaRepository geracaoDiariaRepository;

    @Override
    public BigDecimal buscarGeracaoMensal(String mes, String ano) {
        var geracoes = geracaoDiariaRepository.buscarGeracaoPorMesAno(mes, ano);
        var soma = geracoes.stream().mapToDouble(GeracaoDiaria::getGeracao).sum();
        return BigDecimal.valueOf(soma).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public void salvar(GeracaoDiaria geracaoDiaria) {
        var opt = geracaoDiariaRepository.findByDataGeracao(geracaoDiaria.getDataGeracao());
        opt.ifPresent(diaria -> geracaoDiaria.setId(diaria.getId()));
        geracaoDiariaRepository.save(geracaoDiaria);
    }
}
