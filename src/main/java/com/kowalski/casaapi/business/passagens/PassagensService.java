package com.kowalski.casaapi.business.passagens;

import com.kowalski.casaapi.api.v1.response.PassagensResponse;
import com.kowalski.casaapi.api.v1.response.PassagensResponse.Alerta;
import com.kowalski.casaapi.api.v1.response.PassagensResponse.Ponto;
import com.kowalski.casaapi.api.v1.response.PassagensResponse.Serie;
import com.kowalski.casaapi.business.exception.MonitorIndisponivelException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aba Passagens: lê o schema {@code passagens} do Postgres, preenchido pelo robô de passagens
 * (/opt/projetos/robo-passagens) depois de cada rodada. A API só lê.
 */
@Service
@RequiredArgsConstructor
public class PassagensService {

    /** Acima disto o gráfico mostra o menor preço de cada dia, em vez de um ponto por busca. */
    static final int DIAS_POR_BUSCA = 31;
    /** Folga além de duas rodadas do robô antes de considerar que ele parou. */
    static final Duration FOLGA = Duration.ofMinutes(30);

    private static final String TRECHO = "rota, origem, destino, ida, volta";
    /** Mesmo trecho e datas (volta nula = só ida). */
    private static final String MESMO_TRECHO = "a.rota = s.rota AND a.origem = s.origem AND a.destino = s.destino "
            + "AND a.ida = s.ida AND a.volta IS NOT DISTINCT FROM s.volta";

    private final NamedParameterJdbcTemplate jdbc;

    private record Robo(OffsetDateTime sincronizadoEm, double intervaloHoras, String moeda) { }

    /** @param dias período dos pontos do gráfico; 0 = todo o histórico */
    public PassagensResponse listar(int dias) {
        try {
            return montar(Math.max(0, dias));
        } catch (DataAccessException e) {
            throw new MonitorIndisponivelException("Histórico de passagens indisponível (Postgres)", e);
        }
    }

    private PassagensResponse montar(int dias) {
        List<Robo> estado = jdbc.query(
                "SELECT sincronizado_em, intervalo_horas, moeda FROM passagens.robo WHERE id = 1", Map.of(),
                (rs, i) -> new Robo(rs.getObject(1, OffsetDateTime.class), rs.getDouble(2), rs.getString(3)));
        if (estado.isEmpty()) {
            throw new MonitorIndisponivelException("O robô de passagens ainda não enviou dados", null);
        }
        Robo robo = estado.get(0);
        OffsetDateTime sincronizado = robo.sincronizadoEm();
        double intervalo = robo.intervaloHoras();
        String moeda = robo.moeda();

        List<Map<String, Object>> rotas = jdbc.query(
                "SELECT (config || jsonb_build_object('id', id))::text FROM passagens.rota ORDER BY ordem, id", Map.of(),
                (rs, i) -> json(rs.getString(1)));

        boolean porBusca = dias > 0 && dias <= DIAS_POR_BUSCA;
        Map<String, List<Ponto>> pontos = pontos(dias, porBusca);
        List<Serie> series = jdbc.query("""
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

        List<Alerta> alertas = jdbc.query("""
                SELECT rota, origem, destino, ida, volta, preco, em FROM passagens.alerta
                ORDER BY em DESC LIMIT 100""", Map.of(), (rs, i) -> new Alerta(
                rs.getString("rota"), rs.getString("origem"), rs.getString("destino"),
                rs.getObject("ida", LocalDate.class), rs.getObject("volta", LocalDate.class), rs.getInt("preco"),
                rs.getObject("em", OffsetDateTime.class)));

        return new PassagensResponse(sincronizado, intervalo, moeda,
                desatualizado(sincronizado, intervalo, Instant.now()), dias, porBusca ? "busca" : "dia",
                rotas, series, alertas);
    }

    /** Pontos do gráfico por trecho: cada busca, ou o menor preço de cada dia (fuso de São Paulo). */
    private Map<String, List<Ponto>> pontos(int dias, boolean porBusca) {
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

    static boolean desatualizado(OffsetDateTime sincronizado, double intervaloHoras, Instant agora) {
        if (sincronizado == null) return true;
        double horas = intervaloHoras > 0 ? intervaloHoras : 6;
        Duration validade = Duration.ofMinutes(Math.round(horas * 2 * 60)).plus(FOLGA);
        return sincronizado.toInstant().isBefore(agora.minus(validade));
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
