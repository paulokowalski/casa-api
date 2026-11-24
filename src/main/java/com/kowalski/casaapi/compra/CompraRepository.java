package com.kowalski.casaapi.compra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CompraRepository extends JpaRepository<Compra, UUID> {

    String REGISTRO = " SELECT new com.kowalski.casaapi.compra.CompraRegistroResponse(" +
            " c.id, c.nomeProduto, c.valorProduto, c.dataCompra, c.numeroParcelas, " +
            " upper(p.nome), t.nome, c.dataCadastro) FROM Compra c JOIN c.pessoa p JOIN c.tipoLancamento t ";

    @Query(REGISTRO)
    List<CompraRegistroResponse> listarRegistros();

    @Query(REGISTRO + " WHERE upper(p.nome) = upper(:pessoa) ")
    List<CompraRegistroResponse> listarRegistrosDaPessoa(String pessoa);

    @Query(REGISTRO + " WHERE upper(t.nome) = upper(:cartao) ")
    List<CompraRegistroResponse> listarRegistrosDoCartao(String cartao);

    @Query(REGISTRO + " WHERE upper(p.nome) = upper(:pessoa) AND upper(t.nome) = upper(:cartao) ")
    List<CompraRegistroResponse> listarRegistrosDaPessoaECartao(String pessoa, String cartao);
}
