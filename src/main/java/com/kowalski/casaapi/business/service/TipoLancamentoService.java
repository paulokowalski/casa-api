package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.business.model.TipoLancamento;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface TipoLancamentoService {

    TipoLancamento findById(UUID uuid);

}
