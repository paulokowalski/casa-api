package com.kowalski.casaapi.domain.repository;


import com.kowalski.casaapi.domain.model.Rastreamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RastreamentoRepository extends JpaRepository<Rastreamento, UUID> {

    Optional<Rastreamento> findByCodigo(String codigo);

}