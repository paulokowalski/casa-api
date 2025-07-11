package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.GeracaoDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeracaoDiariaRepository extends JpaRepository<GeracaoDiaria, UUID> {

    Optional<GeracaoDiaria> findByData(LocalDate localDate);

}
