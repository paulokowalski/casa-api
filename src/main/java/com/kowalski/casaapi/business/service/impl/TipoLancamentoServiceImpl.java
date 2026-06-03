package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.model.TipoLancamento;
import com.kowalski.casaapi.business.repository.TipoLancamentoRepository;
import com.kowalski.casaapi.business.service.TipoLancamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TipoLancamentoServiceImpl implements TipoLancamentoService {

    private final TipoLancamentoRepository repository;

    @Override
    public TipoLancamento findById(UUID uuid) {
        return repository.findById(uuid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo Lançamento com uuid: " + uuid + " não encontrado!"));
    }
    
}