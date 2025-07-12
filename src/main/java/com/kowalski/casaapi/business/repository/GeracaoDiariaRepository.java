package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.GeracaoDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeracaoDiariaRepository extends JpaRepository<GeracaoDiaria, UUID> {

    Optional<GeracaoDiaria> findByData(LocalDate localDate);

    @Query(value = """
                    SELECT *
                    FROM geracao_diaria g
                    WHERE EXTRACT(YEAR FROM g.data) = :ano
                      AND EXTRACT(MONTH FROM g.data) = :mes
                    ORDER BY g.data ASC
                """, nativeQuery = true)
    List<GeracaoDiaria> buscarPorAnoMes(@Param("ano") int ano, @Param("mes") int mes);

    @Query(value = """
                    SELECT *
                    FROM geracao_diaria g
                    WHERE EXTRACT(YEAR FROM g.data) = :ano
                    ORDER BY g.data ASC
                """, nativeQuery = true)
    List<GeracaoDiaria> buscarPorAno(@Param("ano") int ano);

}
