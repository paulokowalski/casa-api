package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.business.model.GeracaoDiaria;

import java.util.List;

public interface GeracaoDiariaService {

    GeracaoDiaria salvar(GeracaoDiaria geracaoDiaria);

    List<GeracaoDiaria> buscarPorAnoMes(int ano, int mes);

    List<GeracaoDiaria> buscarPorAno(int ano);

}
