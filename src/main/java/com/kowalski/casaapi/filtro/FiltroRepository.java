package com.kowalski.casaapi.filtro;

import com.kowalski.casaapi.comum.Competencias;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class FiltroRepository {

    private final EntityManager entityManager;

    public List<FiltroResponse> categorias() {
        return pares(" SELECT id, nome FROM categorias ORDER BY nome ", Map.of());
    }

    public List<FiltroResponse> anos() {
        return pares(" select distinct cast(tb01.ano as text), cast(tb01.ano as text) from ( " +
                " select distinct DATE_PART('YEAR', data) ano from compra_parcela cp " +
                " ) tb01 order by cast(tb01.ano as text) desc ", Map.of());
    }

    public List<FiltroResponse> meses(String ano) {
        LocalDate inicio = Competencias.inicioDoAno(ano);
        return pares(" select distinct cast(tb01.mes as text), cast(tb01.mes as text) from (" +
                        " select distinct DATE_PART('MONTH', data) mes from compra_parcela cp " +
                        " where data >= :inicio and data < :fim " +
                        " ) tb01 order by cast(tb01.mes as text) asc ",
                Map.of("inicio", inicio, "fim", inicio.plusYears(1))).stream()
                .map(par -> Integer.parseInt(par.codigo()))
                .sorted(Comparator.naturalOrder())
                .map(mes -> new FiltroResponse("%02d".formatted(mes), Meses.values()[mes].toString()))
                .toList();
    }

    public List<FiltroResponse> pessoas(String ano, String mes) {
        LocalDate inicio = Competencias.de(ano, mes);
        return pares(" select distinct upper(p.nome), upper(p.nome) from compra c " +
                        " join pessoa p on p.id = c.pessoa_id " +
                        " join compra_parcela cp on cp.compra_id = c.id " +
                        " where cp.data >= :inicio and cp.data < :fim ",
                Map.of("inicio", inicio, "fim", inicio.plusMonths(1)));
    }

    public List<FiltroResponse> cartoes(String ano, String mes, String pessoa) {
        LocalDate inicio = Competencias.de(ano, mes);
        return pares(" select distinct t.nome, t.nome from compra c " +
                        " join tipo_lancamento t on t.id = c.cartao_id " +
                        " join pessoa p on p.id = c.pessoa_id " +
                        " join compra_parcela cp on cp.compra_id = c.id " +
                        " where cp.data >= :inicio and cp.data < :fim " +
                        "   AND upper(p.nome) = upper(:pessoa) ",
                Map.of("inicio", inicio, "fim", inicio.plusMonths(1), "pessoa", pessoa));
    }

    public List<FiltroResponse> tiposDeLancamento() {
        return pares(" select distinct id, nome from tipo_lancamento c ORDER BY c.nome ASC ", Map.of());
    }

    private List<FiltroResponse> pares(String sql, Map<String, Object> parametros) {
        var consulta = entityManager.createNativeQuery(sql, Tuple.class);
        parametros.forEach(consulta::setParameter);
        @SuppressWarnings("unchecked")
        List<Tuple> linhas = consulta.getResultList();
        return linhas.stream()
                .map(linha -> new FiltroResponse(String.valueOf(linha.get(0)), (String) linha.get(1)))
                .toList();
    }
}
