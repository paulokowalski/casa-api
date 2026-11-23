package com.kowalski.casaapi.geracao;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GeracaoDiariaRepository extends JpaRepository<GeracaoDiaria, UUID> {

    Optional<GeracaoDiaria> findByData(LocalDate data);

    List<GeracaoDiaria> findByDataBetweenOrderByDataAsc(LocalDate inicio, LocalDate fim);
}
