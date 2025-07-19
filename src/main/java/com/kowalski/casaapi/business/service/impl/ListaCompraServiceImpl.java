package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.business.model.ListaCompra;
import com.kowalski.casaapi.business.repository.ListaCompraRepository;
import com.kowalski.casaapi.business.service.ListaCompraService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListaCompraServiceImpl implements ListaCompraService {

    private final ListaCompraRepository listaCompraRepository;

    public ListaCompraServiceImpl(ListaCompraRepository listaCompraRepository){
        this.listaCompraRepository = listaCompraRepository;
    }

    @Override
    public void salvar(String item) {
        var listaCompra = ListaCompra.builder().item(item).build();
        listaCompraRepository.save(listaCompra);
    }

    @Override
    public List<ListaCompra> buscarTodos() {
        return listaCompraRepository.findAll();
    }

    @Override
    public boolean remover(String item) {
        return listaCompraRepository.findByItem(item)
                .map(listaCompra -> {
                    listaCompraRepository.delete(listaCompra);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public void removerTodos() {
        listaCompraRepository.deleteAll();
    }
}
