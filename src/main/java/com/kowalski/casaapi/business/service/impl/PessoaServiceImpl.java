package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.business.model.Pessoa;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import com.kowalski.casaapi.business.service.PessoaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PessoaServiceImpl implements PessoaService {

    private final PessoaRepository pessoaRepository;

    @Override
    public List<Pessoa> listar() {
        return pessoaRepository.findAll();
    }

    @Override
    public Optional<Pessoa> buscarPorId(Long id) {
        return pessoaRepository.findById(id);
    }

    @Override
    public Pessoa atualizar(Pessoa pessoa, Pessoa pessoaAtualizada) {
        pessoa.setNome(pessoaAtualizada.getNome());
        pessoa.setEmail(pessoaAtualizada.getEmail());
        return salvar(pessoa);
    }

    @Override
    public Pessoa salvar(Pessoa pessoa) {
        return pessoaRepository.save(pessoa);
    }

    @Override
    public void deletar(Pessoa pessoa) {
        pessoaRepository.delete(pessoa);
    }

}
