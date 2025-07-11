package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.business.model.GeracaoMensal;
import com.kowalski.casaapi.business.repository.GeracaoMensalRepository;
import com.kowalski.casaapi.business.service.GeracaoMensalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeracaoMensalServiceImpl implements GeracaoMensalService {

    private final GeracaoMensalRepository geracaoMensalRepository;

    @Override
    public GeracaoMensal salvar(GeracaoMensal geracaoMensal) {
        return geracaoMensalRepository.findByMes(geracaoMensal.getMes()).map(geracao -> {
            geracao.setGeracao(geracao.getGeracao() + geracaoMensal.getGeracao());
            return geracaoMensalRepository.save(geracao);
        }).orElseGet(() -> geracaoMensalRepository.save(geracaoMensal));
    }
}
