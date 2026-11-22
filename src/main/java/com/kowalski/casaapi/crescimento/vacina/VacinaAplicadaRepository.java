package com.kowalski.casaapi.crescimento.vacina;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VacinaAplicadaRepository extends JpaRepository<VacinaAplicada, UUID> {

    List<VacinaAplicada> findByCriancaId(UUID criancaId);

    Optional<VacinaAplicada> findByCriancaIdAndVacinaId(UUID criancaId, String vacinaId);
}
