package com.kowalski.casaapi.domain.repository;

import com.kowalski.casaapi.domain.model.RastreamentoDetalhe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RastreamentoDetalheRepository extends JpaRepository<RastreamentoDetalhe, UUID> { }