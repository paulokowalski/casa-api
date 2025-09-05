package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraResponse;
import com.kowalski.casaapi.business.model.Compra;

import java.util.List;
import java.util.UUID;

public interface CompraService {

    List<Compra> buscarTodos();

    CompraResponse buscarPorMesENome(String ano, String mes, String pessoa, String cartao, String ultimaParcelaSelecionado);

    void salvar(CompraInput compraInput);

    void remover(UUID id);

    void editar(UUID id, CompraInput compraInput);

}
