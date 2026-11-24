package com.kowalski.casaapi.passagens;

import com.kowalski.casaapi.passagens.PassagensResponse.Alerta;
import com.kowalski.casaapi.passagens.PassagensResponse.Ponto;
import com.kowalski.casaapi.passagens.PassagensResponse.Serie;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PassagensRepository {

    private static final String TRECHO = "rota, origem, destino, ida, volta";
    private static final String MESMO_TRECHO = "a.rota = s.rota AND a.origem = s.origem AND a.destino = s.destino "
            + "AND a.ida = s.ida AND a.volta IS NOT DISTINCT FROM s.volta";

    private final NamedParameterJdbcTemplate jdbc;

    public record Robo(OffsetDateTime sincronizadoEm, double intervaloHoras, String moeda) {
    }

    public Optional<Robo> robo() {
        return jdbc.query(
                "SELECT sincronizado_em, intervalo_horas, moeda FROM passagens.robo WHERE id = 1", Map.of(),
                (rs, i) -> new Robo(rs.getObject(1, OffsetDateTime.class), rs.getDouble(2), rs.getString(3)))
                .stream().findFirst();
    }

    public List<Map<String, Object>> rotas() {
        return jdbc.query(
                "SELECT (config || jsonb_build_object('id', id))::text FROM passagens.rota ORDER BY ordem, id", Map.of(),
                (rs, i) -> json(rs.getString(1)));
    }

    public List<Serie> series(Map<String, List<Ponto>> pontos) {
        return jdbc.query("""
                WITH s AS (
                    SELECT %1$s, COUNT(*) AS buscas, MIN(preco) AS minimo, MAX(preco) AS maximo,
                           ROUND(AVG(preco))::int AS media
                    FROM passagens.preco WHERE ida >= CURRENT_DATE GROUP BY %1$s)
                SELECT s.*, u.consultado_em AS u_em, u.preco AS u_preco, u.cias AS u_cias, u.escalas AS u_escalas, u.url,
                       m.consultado_em AS m_em, m.cias AS m_cias, m.escalas AS m_escalas
                FROM s
                CROSS JOIN LATERAL (SELECT * FROM passagens.preco a WHERE %2$s
                                    ORDER BY consultado_em DESC LIMIT 1) u
                CROSS JOIN LATERAL (SELECT * FROM passagens.preco a WHERE %2$s
                                    ORDER BY preco, consultado_em LIMIT 1) m
                ORDER BY s.rota, s.origem, s.destino, s.ida, s.volta
                """.formatted(TRECHO, MESMO_TRECHO), Map.of(), (rs, i) -> new Serie(
                rs.getString("rota"), rs.getString("origem"), rs.getString("destino"),
                rs.getObject("ida", LocalDate.class), rs.getObject("volta", LocalDate.class), rs.getString("url"),
                pontos.getOrDefault(chave(rs), List.of()),
                new Ponto(rs.getObject("u_em", OffsetDateTime.class), rs.getInt("u_preco"), rs.getString("u_cias"),
                        inteiro(rs, "u_escalas")),
                new Ponto(rs.getObject("m_em", OffsetDateTime.class), rs.getInt("minimo"), rs.getString("m_cias"),
                        inteiro(rs, "m_escalas")),
                rs.getInt("maximo"), rs.getInt("media"), rs.getLong("buscas")));
    }

    public List<Alerta> alertas() {
        return jdbc.query("""
                SELECT rota, origem, destino, ida, volta, preco, criado_em FROM passagens.alerta
                ORDER BY criado_em DESC LIMIT 100""", Map.of(), (rs, i) -> new Alerta(
                rs.getString("rota"), rs.getString("origem"), rs.getString("destino"),
                rs.getObject("ida", LocalDate.class), rs.getObject("volta", LocalDate.class), rs.getInt("preco"),
                rs.getObject("criado_em", OffsetDateTime.class)));
    }

    /** Pontos do gráfico por trecho: cada busca, ou o menor preço de cada dia (fuso de São Paulo). */
    public Map<String, List<Ponto>> pontos(int dias, boolean porBusca) {
        String filtro = "ida >= CURRENT_DATE" + (dias > 0 ? " AND consultado_em >= now() - make_interval(days => :dias)" : "");
        String sql = porBusca
                ? "SELECT %s, consultado_em AS em, preco, cias, escalas FROM passagens.preco WHERE %s ORDER BY consultado_em"
                        .formatted(TRECHO, filtro)
                : """
                SELECT %1$s,
                       date_trunc('day', consultado_em AT TIME ZONE 'America/Sao_Paulo') AT TIME ZONE 'America/Sao_Paulo' AS em,
                       MIN(preco) AS preco, (array_agg(cias ORDER BY preco))[1] AS cias,
                       (array_agg(escalas ORDER BY preco))[1] AS escalas
                FROM passagens.preco WHERE %2$s
                GROUP BY %1$s, 6 ORDER BY 6""".formatted(TRECHO, filtro);
        Map<String, List<Ponto>> r = new HashMap<>();
        jdbc.query(sql, new MapSqlParameterSource("dias", dias), rs -> {
            r.computeIfAbsent(chave(rs), k -> new ArrayList<>()).add(new Ponto(
                    rs.getObject("em", OffsetDateTime.class), rs.getInt("preco"), rs.getString("cias"),
                    inteiro(rs, "escalas")));
        });
        return r;
    }

    private static String chave(ResultSet rs) throws SQLException {
        return rs.getString("rota") + "|" + rs.getString("origem") + "|" + rs.getString("destino") + "|"
                + rs.getString("ida") + "|" + rs.getString("volta");
    }

    private static Integer inteiro(ResultSet rs, String coluna) throws SQLException {
        int v = rs.getInt(coluna);
        return rs.wasNull() ? null : v;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> json(String texto) {
        return JsonMapper.shared().readValue(texto, Map.class);
    }
}
