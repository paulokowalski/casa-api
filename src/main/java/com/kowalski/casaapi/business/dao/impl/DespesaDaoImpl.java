package com.kowalski.casaapi.business.dao.impl;

import com.kowalski.casaapi.api.v1.response.DespesaResponse;
import com.kowalski.casaapi.business.dao.DespesaDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import com.kowalski.casaapi.comum.Competencias;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
                    "   AND cp.dataParcela >= :inicio AND cp.dataParcela < :fim ";

        LocalDate inicio = Competencias.de(ano, mes);
        Query query = entityManager.createQuery(sql);
        query.setParameter("nomePessoa", nomePessoa);
        query.setParameter("inicio", inicio);
        query.setParameter("fim", inicio.plusMonths(1));
        List<Object[]> rows = query.getResultList();
        List<DespesaResponse> responses = new ArrayList<>();
        for (Object[] row : rows) {
            responses.add(new DespesaResponse((BigDecimal) row[0], (BigDecimal) row[1], (BigDecimal) row[2]));
        }
        return responses.get(0);
    }

}
