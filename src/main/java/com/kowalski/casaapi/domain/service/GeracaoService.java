package com.kowalski.casaapi.domain.service;

import com.kowalski.casaapi.domain.model.Geracao;

import java.util.List;

public interface GeracaoService {

    Geracao buscarGeracaoAtual();

    List<Geracao> buscarListaGeracaoMes(String mes);

    Geracao buscarGeracaoMes(String mes);

    Geracao buscarGeracaoPorDiaMes(String dia, String mes, String ano);
}
