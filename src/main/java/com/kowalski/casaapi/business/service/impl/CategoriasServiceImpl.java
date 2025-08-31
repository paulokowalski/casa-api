package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.model.Categorias;
import com.kowalski.casaapi.business.repository.CategoriaRepository;
import com.kowalski.casaapi.business.service.CategoriasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoriasServiceImpl implements CategoriasService {

    private final CategoriaRepository categoriaRepository;

    @Override
    public Categorias findByUUid(UUID uuid) {
        return categoriaRepository.findById(uuid).orElseThrow(() -> new RecursoNaoEncontradoException("Categoria com uuid: " + uuid + " não encontrado!"));
    }
}
