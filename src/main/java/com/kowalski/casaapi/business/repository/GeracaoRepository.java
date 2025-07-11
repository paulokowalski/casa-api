package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.Geracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface GeracaoRepository extends JpaRepository<Geracao, UUID> {

    @Query(value = "SELECT * FROM geracao g WHERE DATE(g.data) = :data ORDER BY data ASC", nativeQuery = true)
    List<Geracao> buscarPorData(@Param("data") LocalDate data);

    @Query(value = "SELECT max(g.geracao) FROM geracao g WHERE DATE(g.data) = :data", nativeQuery = true)
    Double buscarUltimaGeracao(@Param("data") LocalDate data);

}
