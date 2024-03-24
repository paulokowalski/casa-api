package com.kowalski.casaapi.domain.repository;

import com.kowalski.casaapi.domain.model.Geracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeracaoRepository extends JpaRepository<Geracao, UUID> {

    Geracao findFirstByOrderByDataCadastroDesc();

    @Query(
            " SELECT DISTINCT ge " +
            " FROM Geracao ge " +
            " WHERE cast(date_part('month', ge.dataCadastro) as text) = remove_zeros_esquerda(:mes) " +
            " ORDER BY ge.dataCadastro ASC "
    )
    List<Geracao> buscarGeracaoPorMes(String mes);

    @Query(
            " SELECT DISTINCT ge " +
                    " FROM Geracao ge " +
                    " WHERE 1 = 1" +
                    " AND cast(date_part('day', ge.dataCadastro) as text) = remove_zeros_esquerda(:dia) " +
                    " AND cast(date_part('month', ge.dataCadastro) as text) = remove_zeros_esquerda(:mes) " +
                    " AND cast(date_part('year', ge.dataCadastro) as text) = remove_zeros_esquerda(:ano) " +
                    " ORDER BY ge.dataCadastro DESC " +
                    " LIMIT 1 "
    )
    Geracao buscarGeracaoPorDiaMesAno(String dia, String mes, String ano);

}
