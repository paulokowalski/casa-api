package com.kowalski.casaapi.crescimento.medida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MedicaoRepository extends JpaRepository<Medicao, UUID> {

    List<Medicao> findByCriancaIdOrderByDataAscCriadoEmAsc(UUID criancaId);
}
