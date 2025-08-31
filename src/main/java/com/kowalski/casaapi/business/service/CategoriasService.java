package com.kowalski.casaapi.business.service;

import com.kowalski.casaapi.business.model.Categorias;

import java.util.UUID;

public interface CategoriasService {

    Categorias findByUUid(UUID uuid);
}
