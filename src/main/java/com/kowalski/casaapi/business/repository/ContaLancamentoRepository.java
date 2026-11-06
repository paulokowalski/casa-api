package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.ContaLancamento;
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

    /** Último valor real (não estimado) antes do mês: base da estimativa de contas de valor variável */
    Optional<ContaLancamento> findFirstByContaIdAndCompetenciaBeforeAndValorEstimadoFalseOrderByCompetenciaDesc(
            UUID conta, LocalDate antes);

    /** Remove meses não pagos a partir de um mês (serão gerados de novo, com os dados atuais da conta) */
    @Modifying
    @Query("delete from ContaLancamento l where l.conta.id = :conta and l.paga = false and l.competencia >= :aPartirDe")
    int removerNaoPagosAPartirDe(@Param("conta") UUID conta, @Param("aPartirDe") LocalDate aPartirDe);

    /** Remove meses não pagos fora do período da conta (após mudar início, fim ou parcelas). Sem fim: passe uma data distante. */
    @Modifying
    @Query("""
            delete from ContaLancamento l
            where l.conta.id = :conta and l.paga = false
              and (l.competencia < :inicio or l.competencia > :fim)
            """)
    int removerNaoPagosForaDoPeriodo(@Param("conta") UUID conta, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /** Atualiza a estimativa dos meses seguintes de uma conta de valor variável */
    @Modifying
    @Query("""
            update ContaLancamento l set l.valor = :valor
            where l.conta.id = :conta and l.competencia > :depoisDe and l.valorEstimado = true and l.paga = false
            """)
    int atualizarEstimativasDepoisDe(@Param("conta") UUID conta, @Param("depoisDe") LocalDate depoisDe,
                                     @Param("valor") BigDecimal valor);
}
