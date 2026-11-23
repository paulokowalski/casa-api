package com.kowalski.casaapi.categoria;

import com.kowalski.casaapi.categoria.Categorias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CategoriaRepository extends JpaRepository<Categorias, UUID> {
}
