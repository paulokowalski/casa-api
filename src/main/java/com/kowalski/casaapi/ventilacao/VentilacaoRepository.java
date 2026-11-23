package com.kowalski.casaapi.ventilacao;

import com.kowalski.casaapi.ventilacao.VentilacaoResponse.Leitura;
import com.kowalski.casaapi.ventilacao.VentilacaoResponse.Limite;
import com.kowalski.casaapi.config.monitor.MonitorConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Ventilação do servidor no MariaDB {@code casa_monitor}: uma linha por minuto com o que o Home Assistant enviou
 * (estado da tomada das ventoinhas, temperaturas e limites). Tabela criada pela API na subida.
 */
@Repository
public class VentilacaoRepository {

    private static final String TABELA = """
            CREATE TABLE IF NOT EXISTS amostra_ventilacao (
                id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                coletado_em  DATETIME    NOT NULL,
                recebido_em  DATETIME    NOT NULL,
                tomada       VARCHAR(16) NOT NULL,
                precisa      BOOLEAN     NULL,
                automatica   BOOLEAN     NULL,
                soc          DOUBLE      NULL,
                hds          DOUBLE      NULL,
                nvme         DOUBLE      NULL,
                liga_soc     DOUBLE      NULL,
                desliga_soc  DOUBLE      NULL,
                liga_hds     DOUBLE      NULL,
                desliga_hds  DOUBLE      NULL,
                liga_nvme    DOUBLE      NULL,
                desliga_nvme DOUBLE      NULL,
                UNIQUE KEY ux_amostra_ventilacao_coletado (coletado_em)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    /** Resumo por dia; "ligou" = minuto ligado cujo minuto gravado anterior não estava ligado. */
    private static final String POR_DIA = """
            SELECT DATE(coletado_em) AS dia, COUNT(*) AS minutos, COALESCE(SUM(tomada = 'on'), 0) AS ligada,
                   COALESCE(SUM(tomada = 'on' AND COALESCE(anterior, '') <> 'on'), 0) AS ligou,
                   MAX(soc) AS max_soc, MAX(hds) AS max_hds, MAX(nvme) AS max_nvme
              FROM (SELECT coletado_em, tomada, soc, hds, nvme,
                           LAG(tomada) OVER (ORDER BY coletado_em) AS anterior
                      FROM amostra_ventilacao
                     WHERE coletado_em >= ? - INTERVAL 1 DAY AND coletado_em < ?) t
             WHERE coletado_em >= ? AND coletado_em < ?
             GROUP BY DATE(coletado_em)
             ORDER BY dia DESC
            """;

    private final JdbcTemplate jdbc;

    public VentilacaoRepository(@Qualifier(MonitorConfig.MONITOR) DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public void criarTabela() {
        jdbc.execute(TABELA);
    }

    /** Grava a leitura no minuto; outra leitura no mesmo minuto (ex.: a tomada mudou) substitui a anterior. */
    public void gravar(LocalDateTime minuto, LocalDateTime recebidoEm, Leitura l) {
        jdbc.update("""
                        INSERT INTO amostra_ventilacao (coletado_em, recebido_em, tomada, precisa, automatica, soc, hds, nvme,
                            liga_soc, desliga_soc, liga_hds, desliga_hds, liga_nvme, desliga_nvme)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON DUPLICATE KEY UPDATE recebido_em = VALUES(recebido_em), tomada = VALUES(tomada),
                            precisa = VALUES(precisa), automatica = VALUES(automatica), soc = VALUES(soc),
                            hds = VALUES(hds), nvme = VALUES(nvme), liga_soc = VALUES(liga_soc),
                            desliga_soc = VALUES(desliga_soc), liga_hds = VALUES(liga_hds),
                            desliga_hds = VALUES(desliga_hds), liga_nvme = VALUES(liga_nvme),
                            desliga_nvme = VALUES(desliga_nvme)
                        """,
                minuto, recebidoEm, l.tomada(), l.precisa(), l.automatica(), l.soc(), l.hds(), l.nvme(),
                liga(l.limiteSoc()), desliga(l.limiteSoc()), liga(l.limiteHds()), desliga(l.limiteHds()),
                liga(l.limiteNvme()), desliga(l.limiteNvme()));
    }

    public Optional<Ultima> ultima() {
        return jdbc.query("SELECT * FROM amostra_ventilacao ORDER BY coletado_em DESC LIMIT 1",
                (rs, i) -> new Ultima(rs.getObject("coletado_em", LocalDateTime.class),
                        rs.getObject("recebido_em", LocalDateTime.class), leitura(rs))).stream().findFirst();
    }

    public record Ultima(LocalDateTime minuto, LocalDateTime recebidoEm, Leitura leitura) {
    }

    /** Primeiro minuto do trecho atual com a tomada em {@code estado} (até {@code ate}). */
    public LocalDateTime desde(String estado, LocalDateTime ate) {
        LocalDateTime outro = jdbc.queryForObject(
                "SELECT MAX(coletado_em) FROM amostra_ventilacao WHERE tomada <> ? AND coletado_em <= ?",
                LocalDateTime.class, estado, ate);
        return outro == null
                ? jdbc.queryForObject("SELECT MIN(coletado_em) FROM amostra_ventilacao", LocalDateTime.class)
                : jdbc.queryForObject("SELECT MIN(coletado_em) FROM amostra_ventilacao WHERE coletado_em > ?",
                LocalDateTime.class, outro);
    }

    /** Minutos gravados no período, em ordem: [minuto, leitura]. */
    public List<Minuto> minutos(LocalDateTime de, LocalDateTime ate) {
        return jdbc.query("SELECT * FROM amostra_ventilacao WHERE coletado_em >= ? AND coletado_em < ? ORDER BY coletado_em",
                (rs, i) -> new Minuto(rs.getObject("coletado_em", LocalDateTime.class), leitura(rs)), de, ate);
    }

    public List<VentilacaoResponse.ResumoDia> porDia(LocalDateTime de, LocalDateTime ate) {
        return jdbc.query(POR_DIA, (rs, i) -> new VentilacaoResponse.ResumoDia(rs.getObject("dia", LocalDate.class),
                rs.getLong("ligada"), rs.getLong("minutos"), rs.getLong("ligou"),
                numero(rs, "max_soc"), numero(rs, "max_hds"), numero(rs, "max_nvme")), de, ate, de, ate);
    }

    public int apagarAntesDe(LocalDateTime limite) {
        return jdbc.update("DELETE FROM amostra_ventilacao WHERE coletado_em < ?", limite);
    }

    public record Minuto(LocalDateTime em, Leitura leitura) {
    }

    private static Leitura leitura(ResultSet rs) throws SQLException {
        return new Leitura(rs.getString("tomada"), logico(rs, "precisa"), logico(rs, "automatica"),
                numero(rs, "soc"), numero(rs, "hds"), numero(rs, "nvme"),
                new Limite(numero(rs, "liga_soc"), numero(rs, "desliga_soc")),
                new Limite(numero(rs, "liga_hds"), numero(rs, "desliga_hds")),
                new Limite(numero(rs, "liga_nvme"), numero(rs, "desliga_nvme")));
    }

    private static Double liga(Limite l) {
        return l == null ? null : l.liga();
    }

    private static Double desliga(Limite l) {
        return l == null ? null : l.desliga();
    }

    private static Boolean logico(ResultSet rs, String coluna) throws SQLException {
        boolean v = rs.getBoolean(coluna);
        return rs.wasNull() ? null : v;
    }

    private static Double numero(ResultSet rs, String coluna) throws SQLException {
        double v = rs.getDouble(coluna);
        return rs.wasNull() ? null : Math.round(v * 10) / 10.0;
    }
}
