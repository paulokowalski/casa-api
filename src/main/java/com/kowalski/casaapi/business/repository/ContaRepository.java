package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.Conta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContaRepository extends JpaRepository<Conta, UUID> {

    List<Conta> findByPessoaIdOrderByDescricaoAsc(Long pessoaId);

    @Query("""
            select c from Conta c
            where c.pessoa.id = :pessoaId
              and c.competenciaInicio <= :ate
              and (c.competenciaFim is null or c.competenciaFim >= :de)
            """)
    List<Conta> ativasNoPeriodo(@Param("pessoaId") Long pessoaId, @Param("de") LocalDate de, @Param("ate") LocalDate ate);
}
