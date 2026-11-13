package com.kowalski.casaapi.business.dao.impl;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import com.kowalski.casaapi.business.dao.DespesaDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DespesaDaoImpl implements DespesaDao {

    private final EntityManager entityManager;

    public DespesaResponse findByNomePessoaAndMesAnoReferencia(String nomePessoa, String mes, String ano) {
        var sql =   " SELECT " +
                    " sum(cp.valorParcela) valorMes, " +
                    " sum(case when c.numeroParcelas > 1 then cp.valorParcela else 0 end) comprasParceladas, " +
                    " sum(case when cp.numeroParcela = 1 and c.numeroParcelas = 1 and cp.valorParcela > 0 then cp.valorParcela else 0 end) gastosNoCartao " +
                    " From CompraParcela cp" +
                    " JOIN cp.compra c " +
                    " WHERE UPPER(c.nomePessoaCompra) = UPPER(:nomePessoa) " +
                    "   AND cast(date_part('month', cp.dataParcela) as text) = cast(remove_zeros_esquerda(:mes) as text) " +
                    "   AND cast(date_part('year', cp.dataParcela) as text) = cast(remove_zeros_esquerda(:ano) as text) ";

        Query query = entityManager.createQuery(sql);
        query.setParameter("nomePessoa", nomePessoa);
        query.setParameter("mes", mes);
        query.setParameter("ano", ano);
        List<Object[]> rows = query.getResultList();
        List<DespesaResponse> responses = new ArrayList<>();
        for (Object[] row : rows) {
            responses.add(new DespesaResponse((BigDecimal) row[0], (BigDecimal) row[1], (BigDecimal) row[2]));
        }
        return responses.get(0);
    }

}
