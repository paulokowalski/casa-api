package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.input.ContaInput;
import com.kowalski.casaapi.api.v1.input.LancamentoInput;
import com.kowalski.casaapi.api.v1.response.ContaResponse;
import com.kowalski.casaapi.api.v1.response.LancamentoResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ContaService {

    List<ContaResponse> listar(Long pessoaId);

    ContaResponse criar(ContaInput input);

    ContaResponse editar(UUID id, ContaInput input, LocalDate aPartirDe);

    void excluir(UUID id, LocalDate aPartirDe);

    List<LancamentoResponse> lancamentos(Long pessoaId, LocalDate de, LocalDate ate);

    LancamentoResponse atualizarLancamento(UUID id, LancamentoInput input);
}
