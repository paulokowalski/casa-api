package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.dto.GeradoDTO;
import com.kowalski.casaapi.api.v1.response.GeracaoDiariaDTO;

import java.time.LocalDate;

public interface GeracaoService {

    GeracaoDiariaDTO buscarGeracao(LocalDate data);

    void salvar(GeradoDTO dto);

}
