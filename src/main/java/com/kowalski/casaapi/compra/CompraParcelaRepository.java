package com.kowalski.casaapi.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CompraParcelaRepository extends JpaRepository<CompraParcela, UUID> {
    @Query( " SELECT cp FROM CompraParcela cp " +
            " JOIN FETCH cp.compra c " +
            " JOIN FETCH c.pessoa p " +
            " JOIN FETCH c.tipoLancamento t " +
            " LEFT JOIN FETCH c.categoria " +
            " WHERE cp.dataParcela >= :inicio AND cp.dataParcela < :fim " +
            " AND upper(p.nome) = upper(:pessoa) " +
            " ORDER BY c.dataCadastro DESC "
    )
    List<CompraParcela> buscarPorPeriodo(LocalDate inicio, LocalDate fim, String pessoa);

    @Query( " SELECT cp FROM CompraParcela cp " +
            " JOIN FETCH cp.compra c " +
            " JOIN FETCH c.pessoa p " +
            " JOIN FETCH c.tipoLancamento t " +
            " LEFT JOIN FETCH c.categoria " +
            " WHERE cp.dataParcela >= :inicio AND cp.dataParcela < :fim " +
            " AND upper(p.nome) = upper(:pessoa) " +
            " AND upper(t.nome) like upper(:cartao) " +
            " ORDER BY c.dataCadastro DESC "
    )
    List<CompraParcela> buscarPorPeriodoECartao(LocalDate inicio, LocalDate fim, String pessoa, String cartao);

    @Modifying(flushAutomatically = true)
    @Query("delete from CompraParcela cp where cp.compra.id = :compraId")
    void apagarParcelasDaCompra(UUID compraId);
}
