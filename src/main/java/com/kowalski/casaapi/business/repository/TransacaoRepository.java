package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

    @Query("SELECT t FROM Transacao t WHERE t.pessoa.id = :pessoaId AND YEAR(t.data) = :ano AND MONTH(t.data) = :mes")
    List<Transacao> findByPessoaIdAndDataAnoAndDataMes(
            @Param("pessoaId") Long pessoaId,
            @Param("ano") Integer ano,
            @Param("mes") Integer mes
    );
    
    List<Transacao> findByIdSerie(String idSerie);

    @Query("SELECT t FROM Transacao t WHERE t.idSerie = :idSerie AND t.data >= :data")
    List<Transacao> findByIdSerieAndDataFrom(
            @Param("idSerie") String idSerie,
            @Param("data") LocalDate data
    );

    @Query("SELECT t FROM Transacao t WHERE t.data BETWEEN :dataInicio AND :dataFim AND t.paga = false AND t.tipo = 'despesa' ORDER BY t.data ASC ")
    List<Transacao> findByData(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );
}