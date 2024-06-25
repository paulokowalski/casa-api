package com.kowalski.casaapi.domain.service.impl;

import com.kowalski.casaapi.domain.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.domain.model.Cartao;
import com.kowalski.casaapi.domain.repository.CartaoRepository;
import com.kowalski.casaapi.domain.service.CartaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartaoServiceImpl implements CartaoService {

    private final CartaoRepository repository;

    @Override
    public Cartao findByNome(String nome) {
        return repository.findByNome(nome)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cartão com nome: " + nome + " não encontrado!"));
    }
    
}