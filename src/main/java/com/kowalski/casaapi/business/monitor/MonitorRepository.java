package com.kowalski.casaapi.business.monitor;

import com.kowalski.casaapi.api.v1.response.ServidorDiaResponse;
import com.kowalski.casaapi.api.v1.response.ServidorDiscoResponse;
import com.kowalski.casaapi.api.v1.response.ServidorEstatisticasResponse;
import com.kowalski.casaapi.api.v1.response.ServidorEstatisticasResponse.Faixa;
import com.kowalski.casaapi.api.v1.response.ServidorResumoDiaResponse;
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

/**
 * Histórico do monitoramento no MariaDB (banco {@code casa_monitor}). As tabelas são criadas pela própria
 * API na subida ({@link #criarTabelas()}), sem migration manual.
 */
@Repository
public class MonitorRepository {

    private static final String TABELA_AMOSTRA = """
            CREATE TABLE IF NOT EXISTS amostra_servidor (
                id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                coletado_em       DATETIME NOT NULL,
                intervalo_seg     INT      NOT NULL,
                cpu_pct           DOUBLE   NOT NULL,
                iowait_pct        DOUBLE   NOT NULL,
                load1             DOUBLE   NOT NULL,
                load5             DOUBLE   NOT NULL,
                load15            DOUBLE   NOT NULL,
                processos         INT      NOT NULL,
                mem_total         BIGINT   NOT NULL,
                mem_usada         BIGINT   NOT NULL,
                mem_pct           DOUBLE   NOT NULL,
                swap_total        BIGINT   NOT NULL,
                swap_usada        BIGINT   NOT NULL,
                swap_pct          DOUBLE   NOT NULL,
                temp_max          DOUBLE   NULL,
                temp_soc          DOUBLE   NULL,
                temp_cpu          DOUBLE   NULL,
                temp_gpu          DOUBLE   NULL,
                rede_rx_bps       DOUBLE   NOT NULL,
                rede_tx_bps       DOUBLE   NOT NULL,
                disco_leitura_bps DOUBLE   NOT NULL,
                disco_escrita_bps DOUBLE   NOT NULL,
                KEY ix_amostra_servidor_coletado (coletado_em)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String TABELA_DISCO = """
            CREATE TABLE IF NOT EXISTS amostra_disco (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                coletado_em DATETIME     NOT NULL,
                nome        VARCHAR(60)  NOT NULL,
                caminho     VARCHAR(255) NOT NULL,
                total       BIGINT       NOT NULL,
                usado       BIGINT       NOT NULL,
                pct         DOUBLE       NOT NULL,
                KEY ix_amostra_disco_coletado (coletado_em, nome)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String INSERIR = """
            INSERT INTO amostra_servidor (coletado_em, intervalo_seg, cpu_pct, iowait_pct, load1, load5, load15,
                processos, mem_total, mem_usada, mem_pct, swap_total, swap_usada, swap_pct,
                temp_max, temp_soc, temp_cpu, temp_gpu, rede_rx_bps, rede_tx_bps, disco_leitura_bps, disco_escrita_bps)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    /** Agregados usados tanto no resumo do dia quanto na lista de dias. */
    private static final String AGREGADOS = """
            COUNT(*) AS amostras,
            MIN(cpu_pct) AS cpu_min, AVG(cpu_pct) AS cpu_med, MAX(cpu_pct) AS cpu_max,
            MIN(mem_pct) AS mem_min, AVG(mem_pct) AS mem_med, MAX(mem_pct) AS mem_max,
            MIN(temp_max) AS temp_min, AVG(temp_max) AS temp_med, MAX(temp_max) AS temp_maxima,
            MIN(load1) AS load_min, AVG(load1) AS load_med, MAX(load1) AS load_max,
            MAX(swap_pct) AS swap_max, AVG(iowait_pct) AS iowait_med,
            SUM(rede_rx_bps * intervalo_seg) AS rede_rx, SUM(rede_tx_bps * intervalo_seg) AS rede_tx,
            SUM(disco_leitura_bps * intervalo_seg) AS disco_lido, SUM(disco_escrita_bps * intervalo_seg) AS disco_escrito
            """;

    private static final String SERIE = """
            SELECT HOUR(coletado_em) * 60 + FLOOR(MINUTE(coletado_em) / 5) * 5 AS minuto,
                   AVG(cpu_pct) AS cpu, MAX(cpu_pct) AS cpu_max, AVG(mem_pct) AS mem, AVG(swap_pct) AS swap,
                   AVG(temp_max) AS temp, MAX(temp_max) AS temp_max, AVG(load1) AS carga, AVG(iowait_pct) AS iowait,
                   AVG(rede_rx_bps) AS rx, AVG(rede_tx_bps) AS tx,
                   AVG(disco_leitura_bps) AS leitura, AVG(disco_escrita_bps) AS escrita
              FROM amostra_servidor
             WHERE coletado_em >= ? AND coletado_em < ?
             GROUP BY minuto
             ORDER BY minuto
            """;

    private static final String DISCOS_DO_DIA = """
            SELECT d.nome, d.caminho, d.total, d.usado, d.pct
              FROM amostra_disco d
              JOIN (SELECT nome, MAX(coletado_em) AS ultima
                      FROM amostra_disco
                     WHERE coletado_em >= ? AND coletado_em < ?
                     GROUP BY nome) u ON u.nome = d.nome AND u.ultima = d.coletado_em
             ORDER BY d.id
            """;

    private final JdbcTemplate jdbc;

    public MonitorRepository(@Qualifier(MonitorConfig.MONITOR) DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public void criarTabelas() {
        jdbc.execute(TABELA_AMOSTRA);
        jdbc.execute(TABELA_DISCO);
    }

    public void inserir(Amostra a) {
        jdbc.update(INSERIR, a.coletadoEm(), a.intervaloSeg(), a.cpuPct(), a.iowaitPct(), a.load1(), a.load5(),
                a.load15(), a.processos(), a.memTotal(), a.memUsada(), a.memPct(), a.swapTotal(), a.swapUsada(),
                a.swapPct(), a.tempMax(), a.tempSoc(), a.tempCpu(), a.tempGpu(), a.redeRxBps(), a.redeTxBps(),
                a.discoLeituraBps(), a.discoEscritaBps());
    }

    public void inserirDiscos(LocalDateTime coletadoEm, List<LeitorSistema.EspacoDisco> discos) {
        if (discos.isEmpty()) return;
        jdbc.batchUpdate("INSERT INTO amostra_disco (coletado_em, nome, caminho, total, usado, pct) VALUES (?, ?, ?, ?, ?, ?)",
                discos.stream().map(d -> new Object[]{coletadoEm, d.nome(), d.caminho(), d.total(), d.usado(), d.pct()}).toList());
    }

    public ServidorEstatisticasResponse resumo(LocalDateTime de, LocalDateTime ate) {
        return jdbc.queryForObject("SELECT " + AGREGADOS + " FROM amostra_servidor WHERE coletado_em >= ? AND coletado_em < ?",
                (rs, i) -> estatisticas(rs), de, ate);
    }

    public List<ServidorDiaResponse.Ponto> serie(LocalDateTime de, LocalDateTime ate) {
        return jdbc.query(SERIE, (rs, i) -> {
            int minuto = rs.getInt("minuto");
            return new ServidorDiaResponse.Ponto(
                    "%02d:%02d".formatted(minuto / 60, minuto % 60),
                    rs.getDouble("cpu"), rs.getDouble("cpu_max"), rs.getDouble("mem"), rs.getDouble("swap"),
                    numero(rs, "temp"), numero(rs, "temp_max"), rs.getDouble("carga"), rs.getDouble("iowait"),
                    rs.getDouble("rx"), rs.getDouble("tx"), rs.getDouble("leitura"), rs.getDouble("escrita"));
        }, de, ate);
    }

    public List<ServidorDiscoResponse> discos(LocalDateTime de, LocalDateTime ate) {
        return jdbc.query(DISCOS_DO_DIA, (rs, i) -> new ServidorDiscoResponse(rs.getString("nome"), rs.getString("caminho"),
                rs.getLong("total"), rs.getLong("usado"), rs.getDouble("pct")), de, ate);
    }

    /** Um item por dia com amostras, do mais recente para o mais antigo. */
    public List<ServidorResumoDiaResponse> porDia(LocalDate de, LocalDate ate) {
        return jdbc.query("SELECT DATE(coletado_em) AS dia, " + AGREGADOS
                        + " FROM amostra_servidor WHERE coletado_em >= ? AND coletado_em < ?"
                        + " GROUP BY DATE(coletado_em) ORDER BY dia DESC",
                (rs, i) -> new ServidorResumoDiaResponse(rs.getObject("dia", LocalDate.class), estatisticas(rs)),
                de.atStartOfDay(), ate.plusDays(1).atStartOfDay());
    }

    /** Apaga amostras antigas; devolve quantas linhas saíram. */
    public int apagarAntesDe(LocalDateTime limite) {
        return jdbc.update("DELETE FROM amostra_servidor WHERE coletado_em < ?", limite)
                + jdbc.update("DELETE FROM amostra_disco WHERE coletado_em < ?", limite);
    }

    private static ServidorEstatisticasResponse estatisticas(ResultSet rs) throws SQLException {
        return new ServidorEstatisticasResponse(
                rs.getLong("amostras"),
                new Faixa(numero(rs, "cpu_min"), numero(rs, "cpu_med"), numero(rs, "cpu_max")),
                new Faixa(numero(rs, "mem_min"), numero(rs, "mem_med"), numero(rs, "mem_max")),
                new Faixa(numero(rs, "temp_min"), numero(rs, "temp_med"), numero(rs, "temp_maxima")),
                new Faixa(numero(rs, "load_min"), numero(rs, "load_med"), numero(rs, "load_max")),
                numero(rs, "swap_max"), numero(rs, "iowait_med"),
                inteiro(rs, "rede_rx"), inteiro(rs, "rede_tx"), inteiro(rs, "disco_lido"), inteiro(rs, "disco_escrito"));
    }

    private static Double numero(ResultSet rs, String coluna) throws SQLException {
        double v = rs.getDouble(coluna);
        return rs.wasNull() ? null : v;
    }

    private static Long inteiro(ResultSet rs, String coluna) throws SQLException {
        Double v = numero(rs, coluna);
        return v == null ? null : Math.round(v);
    }
}
