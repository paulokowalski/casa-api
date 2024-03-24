package com.kowalski.casaapi.domain.dao;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class DespesaDao {

    @Autowired
    private EntityManager entityManager;

    public DespesaResponse findByNomePessoaAndMesAnoReferencia(String nomePessoa, String mes, String ano) {
        var sql =   " SELECT " +
                    " sum(cp.valorParcela) valorMes, " +
                    " sum(case when cp.numeroParcela <> c.numeroParcelas then cp.valorParcela else 0 end) valorProximoMes, " +
                    " sum(case when cp.numeroParcela = c.numeroParcelas and c.numeroParcelas = 1 then cp.valorParcela else 0 end) valorSaindo, " +
                    " sum(case when cp.numeroParcela = c.numeroParcelas and c.numeroParcelas > 1 then cp.valorParcela else 0 end) valorParcelaSaindo, " +
                    " sum(case when cp.numeroParcela = c.numeroParcelas then cp.valorParcela else 0 end) valorSaindoTotal " +
                    " From CompraParcela cp" +
                    " JOIN cp.compra c " +
                    " WHERE UPPER(c.nomePessoaCompra) = UPPER(:nomePessoa) " +
                    "   AND cast(date_part('month', cp.dataParcela) as text) = remove_zeros_esquerda(:mes) " +
                    "   AND cast(date_part('year', cp.dataParcela) as text) = remove_zeros_esquerda(:ano) ";

        Query query = entityManager.createQuery(sql);
        query.setParameter("nomePessoa", nomePessoa);
        query.setParameter("mes", mes);
        query.setParameter("ano", ano);
        List<Object[]> rows = query.getResultList();
        List<DespesaResponse> responses = new ArrayList<>();
        for (Object[] row : rows) {
            responses.add(new DespesaResponse((BigDecimal) row[0], (BigDecimal) row[1], (BigDecimal) row[2], (BigDecimal) row[3], (BigDecimal) row[4]));
        }
        return responses.get(0);
    }

}
