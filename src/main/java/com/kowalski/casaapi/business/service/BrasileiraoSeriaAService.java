package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.response.RodadaResponse;
import com.kowalski.casaapi.api.v1.response.TabelaResponse;

public interface BrasileiraoSeriaAService {

    TabelaResponse consultarPosicao(Long idTime);

    RodadaResponse consultarRodada(Long idTime);

}
