package com.kowalski.casaapi.domain.service;

import com.kowalski.casaapi.domain.model.GeracaoDiaria;

import java.math.BigDecimal;

public interface GeracaoDiariaService {

    BigDecimal buscarGeracaoMensal(String mes, String ano);

    void salvar(GeracaoDiaria geracaoDiaria);

}