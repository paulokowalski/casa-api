package com.kowalski.casaapi.domain.service;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;

public interface DespesaService {

    DespesaResponse buscarPorAnoMesNome(String ano, String mes, String pessoa);

}
