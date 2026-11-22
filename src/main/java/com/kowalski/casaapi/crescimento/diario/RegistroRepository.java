package com.kowalski.casaapi.crescimento.diario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistroRepository extends JpaRepository<Registro, UUID> {

    List<Registro> findByCriancaIdOrderByDataDescCriadoEmDesc(UUID criancaId);

    Optional<Registro> findByCriancaIdAndAlbumChave(UUID criancaId, String albumChave);

    List<Registro> findByCriancaIdAndAlbumChaveNotNull(UUID criancaId);

    @Query("select min(r.proximaConsulta) from Registro r where r.criancaId = :criancaId and r.proximaConsulta >= :hoje")
    LocalDate proximaConsulta(UUID criancaId, LocalDate hoje);
}
