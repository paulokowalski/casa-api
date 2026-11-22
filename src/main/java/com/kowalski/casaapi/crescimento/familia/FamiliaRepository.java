package com.kowalski.casaapi.crescimento.familia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FamiliaRepository extends JpaRepository<Familia, UUID> {

    @Query("""
            select f from Familia f
            where f.id in (select m.familiaId from FamiliaMembro m where m.usuarioId = :usuarioId)
            order by f.criadoEm
            """)
    List<Familia> doUsuario(Long usuarioId);
}
