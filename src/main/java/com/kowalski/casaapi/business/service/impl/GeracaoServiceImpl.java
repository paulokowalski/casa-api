package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.business.model.Geracao;
import com.kowalski.casaapi.business.repository.GeracaoRepository;
import com.kowalski.casaapi.business.service.GeracaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeracaoServiceImpl implements GeracaoService {

    private final GeracaoRepository geracaoRepository;

    @Override
    public void salvar(Double gerado) {
        var geracao = Geracao.builder().geracao(gerado).build();
        geracaoRepository.save(geracao);
    }
}
