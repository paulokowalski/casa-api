package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoMesAnoResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoDiariaResponse;
import com.kowalski.casaapi.api.v1.response.GeracaoMensalResponse;

import java.time.LocalDate;
import java.util.List;

public interface GeracaoService {

    GeracaoDiariaResponse buscarGeracao(LocalDate data);

    GeracaoMensalResponse buscarGeracaoMensal(int ano, int mes);

    List<GeracaoMesAnoResponse> buscarGeracaoAnual(int ano);

    void salvar(GeradoDTO dto);

}
