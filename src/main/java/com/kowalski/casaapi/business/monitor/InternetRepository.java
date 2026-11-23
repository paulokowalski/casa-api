package com.kowalski.casaapi.business.monitor;

import com.kowalski.casaapi.servidor.ServidorEstatisticasResponse;
import com.kowalski.casaapi.api.v1.response.InternetResponse;
import com.kowalski.casaapi.servidor.ServidorEstatisticasResponse.Faixa;
import com.kowalski.casaapi.config.monitor.MonitorConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Histórico da internet da casa no MariaDB {@code casa_monitor}; tabelas criadas pela API na subida. */
@Repository
public class InternetRepository {

    private static final String TABELA_AMOSTRA = """
            CREATE TABLE IF NOT EXISTS amostra_internet (
                id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                coletado_em           DATETIME NOT NULL,
                online                BOOLEAN  NULL,
                roteador_ms           DOUBLE   NULL,
                roteador_perda        DOUBLE   NULL,
                modem_ms              DOUBLE   NULL,
                modem_perda           DOUBLE   NULL,
                cloudflare_ms         DOUBLE   NULL,
                cloudflare_max_ms     DOUBLE   NULL,
                cloudflare_jitter     DOUBLE   NULL,
                cloudflare_perda      DOUBLE   NULL,
                google_ms             DOUBLE   NULL,
                google_perda          DOUBLE   NULL,
                perda                 DOUBLE   NULL,
                dns_pihole_ok         BOOLEAN  NULL,
                dns_pihole_ms         DOUBLE   NULL,
                dns_cloudflare_ms     DOUBLE   NULL,
                dns_google_ms         DOUBLE   NULL,
                ip_publico            VARCHAR(45) NULL,
                pihole_bloqueadas_pct DOUBLE   NULL,
                pihole_consultas_24h  BIGINT   NULL,
                UNIQUE KEY ux_amostra_internet_coletado (coletado_em)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String TABELA_VELOCIDADE = """
            CREATE TABLE IF NOT EXISTS teste_velocidade (
                id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                testado_em        DATETIME NOT NULL,
                ok                BOOLEAN  NOT NULL,
                download_mbps     DOUBLE   NULL,
                upload_mbps       DOUBLE   NULL,
                latencia_ms       DOUBLE   NULL,
                jitter_ms         DOUBLE   NULL,
                latencia_carga_ms DOUBLE   NULL,
                bytes             BIGINT   NULL,
                erro              VARCHAR(250) NULL,
                UNIQUE KEY ux_teste_velocidade_testado (testado_em)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String AGREGADOS = """
            COUNT(*) AS amostras, COALESCE(SUM(online = FALSE), 0) AS offline,
            MIN(cloudflare_ms) AS cf_min, AVG(cloudflare_ms) AS cf_med, MAX(cloudflare_max_ms) AS cf_max,
            AVG(cloudflare_jitter) AS jitter, AVG(perda) AS perda, COALESCE(SUM(perda > 0), 0) AS min_perda,
            AVG(roteador_ms) AS roteador, AVG(modem_ms) AS modem, AVG(google_ms) AS google,
            AVG(dns_pihole_ms) AS dns_pihole, COALESCE(SUM(dns_pihole_ok = FALSE), 0) AS dns_falha,
            AVG(dns_cloudflare_ms) AS dns_cf, COUNT(DISTINCT ip_publico) AS ips
            """;

    private static final String AGREGADOS_VELOCIDADE = """
            COUNT(*) AS testes, MIN(download_mbps) AS d_min, AVG(download_mbps) AS d_med, MAX(download_mbps) AS d_max,
            MIN(upload_mbps) AS u_min, AVG(upload_mbps) AS u_med, MAX(upload_mbps) AS u_max
            """;

    private static final String SERIE = """
            SELECT HOUR(coletado_em) * 60 + FLOOR(MINUTE(coletado_em) / 5) * 5 AS minuto,
                   AVG(roteador_ms) AS roteador, AVG(modem_ms) AS modem, AVG(cloudflare_ms) AS cf,
                   MAX(cloudflare_max_ms) AS cf_max, AVG(google_ms) AS google, AVG(cloudflare_jitter) AS jitter,
                   AVG(perda) AS perda, 100 * AVG(online = FALSE) AS offline,
                   AVG(dns_pihole_ms) AS dns_pihole, AVG(dns_cloudflare_ms) AS dns_cf, AVG(dns_google_ms) AS dns_google
              FROM amostra_internet
             WHERE coletado_em >= ? AND coletado_em < ?
             GROUP BY minuto
             ORDER BY minuto
            """;

    private final JdbcTemplate jdbc;

    public InternetRepository(@Qualifier(MonitorConfig.MONITOR) DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public void criarTabelas() {
        jdbc.execute(TABELA_AMOSTRA);
        jdbc.execute(TABELA_VELOCIDADE);
    }

    public void inserir(AmostraInternet a) {
        jdbc.update("""
                        INSERT IGNORE INTO amostra_internet (coletado_em, online, roteador_ms, roteador_perda, modem_ms,
                            modem_perda, cloudflare_ms, cloudflare_max_ms, cloudflare_jitter, cloudflare_perda, google_ms,
                            google_perda, perda, dns_pihole_ok, dns_pihole_ms, dns_cloudflare_ms, dns_google_ms, ip_publico,
                            pihole_bloqueadas_pct, pihole_consultas_24h)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                a.coletadoEm(), a.online(), a.roteador().mediaMs(), a.roteador().perdaPct(), a.modem().mediaMs(),
                a.modem().perdaPct(), a.cloudflare().mediaMs(), a.cloudflare().maxMs(), a.cloudflare().jitterMs(),
                a.cloudflare().perdaPct(), a.google().mediaMs(), a.google().perdaPct(), a.perdaInternet(),
                a.pihole().ok(), a.pihole().tempoMs(), a.dnsCloudflare().tempoMs(), a.dnsGoogle().tempoMs(),
                a.ipPublico() == null || a.ipPublico().length() > 45 ? null : a.ipPublico(),
                a.piholeBloqueadasPct(), a.piholeConsultas24h());
    }

    /** Grava os testes novos (os já gravados, mesmo horário, são ignorados). Devolve os que entraram. */
    public List<InternetResponse.TesteVelocidade> inserirTestes(List<InternetResponse.TesteVelocidade> testes) {
        return testes.stream().filter(t -> jdbc.update("""
                        INSERT IGNORE INTO teste_velocidade (testado_em, ok, download_mbps, upload_mbps, latencia_ms,
                            jitter_ms, latencia_carga_ms, bytes, erro)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, t.testadoEm(), t.ok(), t.downloadMbps(), t.uploadMbps(), t.latenciaMs(), t.jitterMs(),
                t.latenciaCargaMs(), t.bytes(), t.erro() == null || t.erro().length() <= 250 ? t.erro() : t.erro().substring(0, 250)) > 0)
                .toList();
    }

    public String ultimoIp() {
        List<String> r = jdbc.queryForList("SELECT ip_publico FROM amostra_internet WHERE ip_publico IS NOT NULL"
                + " ORDER BY coletado_em DESC LIMIT 1", String.class);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Download dos últimos testes que deram certo antes de um horário (para comparar o novo). */
    public List<Double> downloadsAntes(LocalDateTime antes, int quantos) {
        return jdbc.queryForList("SELECT download_mbps FROM teste_velocidade WHERE ok AND testado_em < ?"
                + " ORDER BY testado_em DESC LIMIT ?", Double.class, antes, quantos);
    }

    public List<InternetResponse.TesteVelocidade> testes(LocalDateTime de, LocalDateTime ate) {
        return jdbc.query("SELECT * FROM teste_velocidade WHERE testado_em >= ? AND testado_em < ? ORDER BY testado_em",
                (rs, i) -> new InternetResponse.TesteVelocidade(rs.getObject("testado_em", LocalDateTime.class),
                        rs.getBoolean("ok"), numero(rs, "download_mbps"), numero(rs, "upload_mbps"),
                        numero(rs, "latencia_ms"), numero(rs, "jitter_ms"), numero(rs, "latencia_carga_ms"),
                        inteiro(rs, "bytes"), rs.getString("erro")), de, ate);
    }

    public InternetResponse.Resumo resumo(LocalDateTime de, LocalDateTime ate) {
        Object[] v = jdbc.queryForObject("SELECT " + AGREGADOS_VELOCIDADE
                        + " FROM teste_velocidade WHERE ok AND testado_em >= ? AND testado_em < ?",
                (rs, i) -> velocidade(rs), de, ate);
        return jdbc.queryForObject("SELECT " + AGREGADOS + " FROM amostra_internet WHERE coletado_em >= ? AND coletado_em < ?",
                (rs, i) -> resumo(rs, v), de, ate);
    }

    public Map<LocalDate, InternetResponse.Resumo> porDia(LocalDateTime de, LocalDateTime ate) {
        Map<LocalDate, Object[]> velocidades = new LinkedHashMap<>();
        jdbc.query("SELECT DATE(testado_em) AS dia, " + AGREGADOS_VELOCIDADE
                        + " FROM teste_velocidade WHERE ok AND testado_em >= ? AND testado_em < ? GROUP BY DATE(testado_em)",
                rs -> {
                    velocidades.put(rs.getObject("dia", LocalDate.class), velocidade(rs));
                }, de, ate);
        Map<LocalDate, InternetResponse.Resumo> r = new LinkedHashMap<>();
        jdbc.query("SELECT DATE(coletado_em) AS dia, " + AGREGADOS
                        + " FROM amostra_internet WHERE coletado_em >= ? AND coletado_em < ?"
                        + " GROUP BY DATE(coletado_em) ORDER BY dia DESC",
                rs -> {
                    LocalDate dia = rs.getObject("dia", LocalDate.class);
                    r.put(dia, resumo(rs, velocidades.getOrDefault(dia, new Object[]{0L, new Faixa(null, null, null), new Faixa(null, null, null)})));
                }, de, ate);
        return r;
    }

    public List<InternetResponse.Ponto> serie(LocalDateTime de, LocalDateTime ate) {
        return jdbc.query(SERIE, (rs, i) -> {
            int minuto = rs.getInt("minuto");
            return new InternetResponse.Ponto("%02d:%02d".formatted(minuto / 60, minuto % 60),
                    numero(rs, "roteador"), numero(rs, "modem"), numero(rs, "cf"), numero(rs, "cf_max"),
                    numero(rs, "google"), numero(rs, "jitter"), numero(rs, "perda"), rs.getDouble("offline"),
                    numero(rs, "dns_pihole"), numero(rs, "dns_cf"), numero(rs, "dns_google"));
        }, de, ate);
    }

    public int apagarAntesDe(LocalDateTime limite) {
        return jdbc.update("DELETE FROM amostra_internet WHERE coletado_em < ?", limite);
    }

    private static Object[] velocidade(ResultSet rs) throws SQLException {
        return new Object[]{rs.getLong("testes"),
                new Faixa(numero(rs, "d_min"), numero(rs, "d_med"), numero(rs, "d_max")),
                new Faixa(numero(rs, "u_min"), numero(rs, "u_med"), numero(rs, "u_max"))};
    }

    private static InternetResponse.Resumo resumo(ResultSet rs, Object[] v) throws SQLException {
        return new InternetResponse.Resumo(rs.getLong("amostras"), rs.getLong("offline"),
                new Faixa(numero(rs, "cf_min"), numero(rs, "cf_med"), numero(rs, "cf_max")),
                numero(rs, "jitter"), numero(rs, "perda"), rs.getLong("min_perda"),
                numero(rs, "roteador"), numero(rs, "modem"), numero(rs, "google"),
                numero(rs, "dns_pihole"), rs.getLong("dns_falha"), numero(rs, "dns_cf"), rs.getLong("ips"),
                (Long) v[0], (Faixa) v[1], (Faixa) v[2]);
    }

    private static Double numero(ResultSet rs, String coluna) throws SQLException {
        double v = rs.getDouble(coluna);
        return rs.wasNull() ? null : Math.round(v * 10) / 10.0;
    }

    private static Long inteiro(ResultSet rs, String coluna) throws SQLException {
        long v = rs.getLong(coluna);
        return rs.wasNull() ? null : v;
    }
}
