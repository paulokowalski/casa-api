package com.kowalski.casaapi.domain.service;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;

import java.util.List;

public interface FiltroService {

    List<FiltroResponse> buscarAnos();
    List<FiltroResponse> buscarMeses(String ano);
    List<FiltroResponse> buscarPessoas(String ano, String mes);
    List<FiltroResponse> buscarCartoes(String ano, String mes, String pessoa);
}
