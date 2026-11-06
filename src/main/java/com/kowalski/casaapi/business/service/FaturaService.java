package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.input.FaturaInput;
import com.kowalski.casaapi.api.v1.response.FaturaResponse;

import java.time.LocalDate;

public interface FaturaService {

    FaturaResponse obter(Long pessoaId, LocalDate competencia);

    FaturaResponse salvar(Long pessoaId, LocalDate competencia, FaturaInput input);
}
