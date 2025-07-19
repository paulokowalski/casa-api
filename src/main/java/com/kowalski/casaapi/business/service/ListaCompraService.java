package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.business.model.ListaCompra;

import java.util.List;

public interface ListaCompraService {

    void salvar(String item);
    List<ListaCompra> buscarTodos();
    boolean remover(String item);
    void removerTodos();

}
