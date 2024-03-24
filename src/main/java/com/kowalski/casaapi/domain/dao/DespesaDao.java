package com.kowalski.casaapi.domain.dao;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;

public interface DespesaDao {

    DespesaResponse findByNomePessoaAndMesAnoReferencia(String nomePessoa, String mes, String ano);

}
