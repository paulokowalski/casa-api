package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.business.model.CompraParcela;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CompraParcelaRepository extends JpaRepository<CompraParcela, UUID> {

    // Período fechado no início e aberto no fim: [inicio, fim). Comparar a data direto
    // permite usar o índice de dt_parcela.
    // A compra e a categoria vêm no mesmo SELECT (JOIN FETCH) para não disparar
    // uma consulta extra por compra ao montar a resposta.
    @Query( " SELECT cp FROM CompraParcela cp " +
            " JOIN FETCH cp.compra c " +
            " LEFT JOIN FETCH c.categoria " +
            " WHERE cp.dataParcela >= :inicio AND cp.dataParcela < :fim " +
            " AND upper(c.nomePessoaCompra) = upper(:pessoa) " +
            " ORDER BY c.dataCadastro DESC "
    )
    List<CompraParcela> buscarPorPeriodo(LocalDate inicio, LocalDate fim, String pessoa);

    @Query( " SELECT cp FROM CompraParcela cp " +
            " JOIN FETCH cp.compra c " +
            " LEFT JOIN FETCH c.categoria " +
            " WHERE cp.dataParcela >= :inicio AND cp.dataParcela < :fim " +
            " AND upper(c.nomePessoaCompra) = upper(:pessoa) " +
            " AND upper(c.nomeCartao) like upper(:cartao) " +
            " ORDER BY c.dataCadastro DESC "
    )
    List<CompraParcela> buscarPorPeriodoECartao(LocalDate inicio, LocalDate fim, String pessoa, String cartao);

    void deleteByCompraId(UUID compraId);
}
