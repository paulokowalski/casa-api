package com.kowalski.casaapi.business.dao;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;

import java.util.List;

public interface FiltroDao {

    List<FiltroResponse> buscarAnos();
    List<FiltroResponse> buscarMeses(String ano);
    List<FiltroResponse> buscarPessoas(String ano, String mes);
    List<FiltroResponse> buscarCartoes(String ano, String mes, String pessoa);
}
