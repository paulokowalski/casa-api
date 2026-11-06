package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.input.RendaInput;
import com.kowalski.casaapi.api.v1.response.RendaResponse;

import java.time.LocalDate;

public interface RendaService {

    RendaResponse obter(Long pessoaId, LocalDate competencia);

    RendaResponse salvar(Long pessoaId, LocalDate competencia, RendaInput input);
}
