package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.RendaMensal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RendaMensalRepository extends JpaRepository<RendaMensal, RendaMensal.Id> {

    /** Renda do mês ou, se não informada, a mais recente antes dele */
    Optional<RendaMensal> findFirstByIdPessoaIdAndIdCompetenciaLessThanEqualOrderByIdCompetenciaDesc(
            Long pessoaId, LocalDate competencia);
}
