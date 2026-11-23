package com.kowalski.casaapi.conta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContaLancamentoRepository extends JpaRepository<ContaLancamento, UUID> {

    @Query("""
            select l from ContaLancamento l
            where l.conta.id in :contas and l.competencia between :de and :ate
            order by l.competencia, l.vencimento, l.conta.descricao
            """)
    List<ContaLancamento> doPeriodo(@Param("contas") Collection<UUID> contas, @Param("de") LocalDate de, @Param("ate") LocalDate ate);

    Optional<ContaLancamento> findFirstByContaIdAndCompetenciaBeforeAndValorEstimadoFalseOrderByCompetenciaDesc(
            UUID conta, LocalDate antes);

    @Modifying
    @Query("delete from ContaLancamento l where l.conta.id = :conta and l.paga = false and l.competencia >= :aPartirDe")
    int removerNaoPagosAPartirDe(@Param("conta") UUID conta, @Param("aPartirDe") LocalDate aPartirDe);

    @Modifying
    @Query("""
            delete from ContaLancamento l
            where l.conta.id = :conta and l.paga = false
              and (l.competencia < :inicio or l.competencia > :fim)
            """)
    int removerNaoPagosForaDoPeriodo(@Param("conta") UUID conta, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Modifying
    @Query("""
            update ContaLancamento l set l.valor = :valor
            where l.conta.id = :conta and l.competencia > :depoisDe and l.valorEstimado = true and l.paga = false
            """)
    int atualizarEstimativasDepoisDe(@Param("conta") UUID conta, @Param("depoisDe") LocalDate depoisDe,
                                     @Param("valor") BigDecimal valor);
}
