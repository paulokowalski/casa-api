package com.kowalski.casaapi.crescimento.ia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumoMesRepository extends JpaRepository<ResumoMes, UUID> {

    List<ResumoMes> findByCriancaIdOrderByMesDesc(UUID criancaId);

    Optional<ResumoMes> findByCriancaIdAndMes(UUID criancaId, int mes);
}
