package com.kowalski.casaapi.domain.repository;

import com.kowalski.casaapi.domain.model.GeracaoDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeracaoDiariaRepository extends JpaRepository<GeracaoDiaria, UUID> {

    @Query(
            " SELECT DISTINCT ge " +
                    " FROM GeracaoDiaria ge " +
                    " WHERE 1 = 1" +
                    " AND cast(date_part('month', ge.dataGeracao) as text) = remove_zeros_esquerda(:mes) " +
                    " AND cast(date_part('year', ge.dataGeracao) as text) = remove_zeros_esquerda(:ano) " +
                    " ORDER BY ge.dataGeracao ASC "
    )
    List<GeracaoDiaria> buscarGeracaoPorMesAno(String mes, String ano);

    Optional<GeracaoDiaria> findByDataGeracao(LocalDate localDate);
}
