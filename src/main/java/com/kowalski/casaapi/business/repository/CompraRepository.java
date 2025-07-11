package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompraRepository extends JpaRepository<Compra, UUID> { }
