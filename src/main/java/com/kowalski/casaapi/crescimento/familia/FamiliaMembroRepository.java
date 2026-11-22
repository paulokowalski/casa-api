package com.kowalski.casaapi.crescimento.familia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamiliaMembroRepository extends JpaRepository<FamiliaMembro, UUID> {

    Optional<FamiliaMembro> findByFamiliaIdAndUsuarioId(UUID familiaId, Long usuarioId);

    List<FamiliaMembro> findByFamiliaIdOrderByCriadoEm(UUID familiaId);

    List<FamiliaMembro> findByUsuarioId(Long usuarioId);

    long countByFamiliaIdAndPapel(UUID familiaId, PapelFamilia papel);
}
