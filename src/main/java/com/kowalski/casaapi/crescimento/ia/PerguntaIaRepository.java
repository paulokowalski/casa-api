package com.kowalski.casaapi.crescimento.ia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PerguntaIaRepository extends JpaRepository<PerguntaIa, UUID> {

    List<PerguntaIa> findByCriancaIdOrderByFeitaEmAsc(UUID criancaId);

    List<PerguntaIa> findByCriancaIdAndMesOrderByFeitaEmAsc(UUID criancaId, int mes);

    void deleteByCriancaIdAndMes(UUID criancaId, int mes);
}
