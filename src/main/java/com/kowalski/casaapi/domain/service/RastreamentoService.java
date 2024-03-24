package com.kowalski.casaapi.domain.service;

import com.kowalski.casaapi.domain.model.Rastreamento;

import java.util.List;
import java.util.UUID;

public interface RastreamentoService {

    List<Rastreamento> listar();
    void salvar(Rastreamento rastreamento);

    void atualizar(UUID codigo, Rastreamento rastreamento);

    void deletar(UUID codigo);

    void deletar(String rastreio);
}
