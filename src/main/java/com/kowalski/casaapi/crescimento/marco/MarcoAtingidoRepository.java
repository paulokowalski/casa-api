package com.kowalski.casaapi.crescimento.marco;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarcoAtingidoRepository extends JpaRepository<MarcoAtingido, UUID> {

    List<MarcoAtingido> findByCriancaId(UUID criancaId);

    Optional<MarcoAtingido> findByCriancaIdAndMarcoId(UUID criancaId, String marcoId);
}
