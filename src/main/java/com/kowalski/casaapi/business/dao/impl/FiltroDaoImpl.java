package com.kowalski.casaapi.business.dao.impl;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;
import com.kowalski.casaapi.business.dao.FiltroDao;
import com.kowalski.casaapi.business.enumerator.Meses;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FiltroDaoImpl implements FiltroDao {

    private final EntityManager entityManager;

    public List<FiltroResponse> buscarAnos(){
        String sql = " select distinct cast(tb01.ano as text), cast(tb01.ano as text) from ( " +
                     " select distinct DATE_PART('YEAR', dt_parcela) ano from compra_parcela cp " +
                     " ) tb01 order by cast(tb01.ano as text) desc ";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();
        List<FiltroResponse> responses = new ArrayList<>();
        for (Object[] row : rows) {
            responses.add(new FiltroResponse((String) row[0], (String) row[1]));
        }
        return responses;
    }

    public List<FiltroResponse> buscarMeses(String ano){
        String sql = " select distinct cast(tb01.mes as text), cast(tb01.mes as text) from (" +
                     " select distinct DATE_PART('MONTH', dt_parcela) mes from compra_parcela cp " +
                     " where remove_zeros_esquerda(cast(DATE_PART('YEAR', dt_parcela) as text)) = remove_zeros_esquerda(:ano) " +
                     " ) tb01 order by cast(tb01.mes as text) asc ";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("ano", ano);
        List<Object[]> rows = query.getResultList();
        List<FiltroResponse> responses = new ArrayList<>();
        for (Object[] row : rows) {
            int mesNumero = Integer.parseInt((String) row[0]);
            responses.add(new FiltroResponse(
                String.format("%02d", mesNumero),
                Arrays.asList(Meses.values()).get(mesNumero).toString()
            ));
        }
        responses.sort(Comparator.comparingInt(a -> Integer.parseInt(a.codigo())));
        return responses;
    }

    public List<FiltroResponse> buscarPessoas(String ano, String mes){
        String sql = " select distinct c.nm_pessoa_compra, c.nm_pessoa_compra from compra c " +
                     " join compra_parcela cp on cp.compra_id = c.id " +
                     " where remove_zeros_esquerda(cast(DATE_PART('YEAR', dt_parcela) as text)) = remove_zeros_esquerda(:ano) " +
                     "   and remove_zeros_esquerda(cast(DATE_PART('MONTH', dt_parcela) as text)) = remove_zeros_esquerda(:mes) ";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("ano", ano);
        query.setParameter("mes", mes);
        List<Object[]> rows = query.getResultList();
        List<FiltroResponse> responses = new ArrayList<>();
        for (Object[] row : rows) {
            responses.add(new FiltroResponse((String) row[0], (String) row[1]));
        }
        return responses;
    }

    public List<FiltroResponse> buscarCartoes(String ano, String mes, String pessoa){
        String sql = " select distinct c.nm_cartao, c.nm_cartao from compra c " +
                " join compra_parcela cp on cp.compra_id = c.id " +
                " where remove_zeros_esquerda(cast(DATE_PART('YEAR', dt_parcela) as text)) = remove_zeros_esquerda(:ano) " +
                "   AND remove_zeros_esquerda(cast(DATE_PART('MONTH', dt_parcela) as text)) = remove_zeros_esquerda(:mes) " +
                "   AND upper(c.nm_pessoa_compra) = upper(:pessoa) ";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("ano", ano);
        query.setParameter("mes", mes);
        query.setParameter("pessoa", pessoa);
        List<Object[]> rows = query.getResultList();
        List<FiltroResponse> responses = new ArrayList<>();
        for (Object[] row : rows) {
            responses.add(new FiltroResponse((String) row[0], (String) row[1]));
        }
        return responses;
    }

}