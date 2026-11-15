package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.response.DespesaAnualResponse;

public interface DespesaService {

    
    DespesaAnualResponse buscarDespesasAnuais(String ano, String pessoa);

}
