package com.kowalski.casaapi.geracao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GeracaoRepository extends JpaRepository<Geracao, UUID> {

    List<Geracao> findByDataGreaterThanEqualAndDataLessThanOrderByDataAsc(LocalDateTime inicio, LocalDateTime fim);

    @Query("select max(g.geracao) from Geracao g where g.data >= :inicio and g.data < :fim")
    Double maiorGeracaoEntre(LocalDateTime inicio, LocalDateTime fim);
}
