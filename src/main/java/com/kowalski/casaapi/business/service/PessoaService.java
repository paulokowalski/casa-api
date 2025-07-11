package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.business.model.Pessoa;

import java.util.List;
import java.util.Optional;

public interface PessoaService {

    List<Pessoa> listar();

    Optional<Pessoa> buscarPorId(Long id);

    Pessoa atualizar(Pessoa pessoa, Pessoa pessoaAtualizada);

    Pessoa salvar(Pessoa pessoa);

    void deletar(Pessoa pessoa);

}
