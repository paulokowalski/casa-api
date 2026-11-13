package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.api.v1.input.PessoaInput;
import com.kowalski.casaapi.business.model.Pessoa;

import java.util.List;
import java.util.Optional;

public interface PessoaService {

    List<Pessoa> listar();

    Optional<Pessoa> buscarPorId(Long id);

    Pessoa criar(PessoaInput input);

    Pessoa atualizar(Pessoa pessoa, PessoaInput input);

    void deletar(Pessoa pessoa);

}
