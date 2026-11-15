package com.kowalski.casaapi.business.repository;

import com.kowalski.casaapi.api.v1.response.CompraRegistroResponse;
import com.kowalski.casaapi.business.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompraRepository extends JpaRepository<Compra, UUID> {

    String REGISTRO = " SELECT new com.kowalski.casaapi.api.v1.response.CompraRegistroResponse(" +
            " c.id, c.nomeProduto, c.valorProduto, c.dataCompra, c.numeroParcelas, " +
            " c.nomePessoaCompra, c.nomeCartao, c.dataCadastro) FROM Compra c ";

    @Query(REGISTRO)
    List<CompraRegistroResponse> listarRegistros();

    @Query(REGISTRO + " WHERE upper(c.nomePessoaCompra) = upper(:pessoa) ")
    List<CompraRegistroResponse> listarRegistrosDaPessoa(String pessoa);

    @Query(REGISTRO + " WHERE upper(c.nomeCartao) = upper(:cartao) ")
    List<CompraRegistroResponse> listarRegistrosDoCartao(String cartao);

    @Query(REGISTRO + " WHERE upper(c.nomePessoaCompra) = upper(:pessoa) AND upper(c.nomeCartao) = upper(:cartao) ")
    List<CompraRegistroResponse> listarRegistrosDaPessoaECartao(String pessoa, String cartao);
}
