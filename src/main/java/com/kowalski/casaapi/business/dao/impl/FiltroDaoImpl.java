package com.kowalski.casaapi.business.dao.impl;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;
import com.kowalski.casaapi.business.dao.FiltroDao;
import com.kowalski.casaapi.business.enumerator.Meses;
import com.kowalski.casaapi.util.Competencias;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class FiltroDaoImpl implements FiltroDao {

    private final EntityManager entityManager;

    public List<FiltroResponse> buscarCategorias(){
        String sql = " SELECT id, nome FROM categorias ORDER BY nome ";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();
        List<FiltroResponse> responses = new ArrayList<>();
        for (Object[] row : rows) {
            responses.add(new FiltroResponse(String.valueOf(row[0]), (String) row[1]));
        }
        return responses;
    }

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
                     " where dt_parcela >= :inicio and dt_parcela < :fim " +
                     " ) tb01 order by cast(tb01.mes as text) asc ";
        LocalDate inicio = Competencias.inicioDoAno(ano);
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("inicio", inicio);
        query.setParameter("fim", inicio.plusYears(1));
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
                     " where cp.dt_parcela >= :inicio and cp.dt_parcela < :fim ";
        LocalDate inicio = Competencias.de(ano, mes);
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("inicio", inicio);
        query.setParameter("fim", inicio.plusMonths(1));
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
                " where cp.dt_parcela >= :inicio and cp.dt_parcela < :fim " +
                "   AND upper(c.nm_pessoa_compra) = upper(:pessoa) ";
        LocalDate inicio = Competencias.de(ano, mes);
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("inicio", inicio);
        query.setParameter("fim", inicio.plusMonths(1));
        query.setParameter("pessoa", pessoa);
        List<Object[]> rows = query.getResultList();
        List<FiltroResponse> responses = new ArrayList<>();
        for (Object[] row : rows) {
            responses.add(new FiltroResponse((String) row[0], (String) row[1]));
        }
        return responses;
    }

    public List<FiltroResponse> buscarTiposDeLancamentos(){
        String sql = " select distinct id, nome from tipo_lancamento c ORDER BY c.nome ASC ";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> rows = query.getResultList();
        List<FiltroResponse> responses = new ArrayList<>();
        for (Object[] row : rows) {
            responses.add(new FiltroResponse(String.valueOf(row[0]), (String) row[1]));
        }
        return responses;
    }

}