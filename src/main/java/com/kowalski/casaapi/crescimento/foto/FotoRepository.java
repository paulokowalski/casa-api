package com.kowalski.casaapi.crescimento.foto;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FotoRepository extends JpaRepository<Foto, UUID> {

    Optional<Foto> findByCriancaIdAndSha256(UUID criancaId, String sha256);

    List<Foto> findByCriancaIdOrderByCriadaEmDesc(UUID criancaId);

    List<Foto> findByRegistroIdOrderByOrdemAsc(UUID registroId);

    long countByCriancaId(UUID criancaId);

    List<Foto> findByRegistroIdInOrderByOrdemAsc(Collection<UUID> registroIds);
}
