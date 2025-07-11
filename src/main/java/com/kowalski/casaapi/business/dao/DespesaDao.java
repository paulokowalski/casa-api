package com.kowalski.casaapi.business.dao;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;

public interface DespesaDao {

    DespesaResponse findByNomePessoaAndMesAnoReferencia(String nomePessoa, String mes, String ano);

}
