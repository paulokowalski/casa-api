package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraRegistroResponse;
import com.kowalski.casaapi.api.v1.response.CompraResponse;

import java.util.List;
import java.util.UUID;

public interface CompraService {

    /** Lista as compras; pessoa e cartão são opcionais (nulo = sem filtro). */
    List<CompraRegistroResponse> buscarTodos(String pessoa, String cartao);

    CompraResponse buscarPorMesENome(String ano, String mes, String pessoa, String cartao, String ultimaParcelaSelecionado);

    /** Parcelas de todos os meses entre {@code de} e {@code ate} (aaaa-mm, inclusive). */
    CompraResponse buscarPorPeriodo(String de, String ate, String pessoa, String cartao);

    void salvar(CompraInput compraInput);

    void remover(UUID id);

    void editar(UUID id, CompraInput compraInput);

}
