package com.kowalski.casaapi.renda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RendaMensalRepository extends JpaRepository<RendaMensal, RendaMensal.Id> {

    Optional<RendaMensal> findFirstByIdPessoaIdAndIdCompetenciaLessThanEqualOrderByIdCompetenciaDesc(
            Long pessoaId, LocalDate competencia);
}
