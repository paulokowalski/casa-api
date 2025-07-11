package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import com.kowalski.casaapi.api.v1.response.DespesaAnualResponse;

public interface DespesaService {

    DespesaResponse buscarPorAnoMesNome(String ano, String mes, String pessoa);
    
    DespesaAnualResponse buscarDespesasAnuais(String ano, String pessoa);

}
