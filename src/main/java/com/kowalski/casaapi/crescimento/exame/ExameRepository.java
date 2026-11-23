package com.kowalski.casaapi.crescimento.exame;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExameRepository extends JpaRepository<Exame, UUID> {

    List<Exame> findByCriancaIdOrderByDataDescCriadoEmDesc(UUID criancaId);

    Optional<Exame> findByCriancaIdAndSha256(UUID criancaId, String sha256);
}
