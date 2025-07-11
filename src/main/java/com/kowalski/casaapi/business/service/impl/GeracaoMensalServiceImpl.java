package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.business.model.Geracao;
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
    //TODO: Implementar o cálculo mensal
    public GeracaoMensal salvar(Geracao geracao) {
        return null;
    }
}
