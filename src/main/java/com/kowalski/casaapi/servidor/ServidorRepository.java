package com.kowalski.casaapi.servidor;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Histórico do monitoramento no MariaDB (banco {@code casa_monitor}). As tabelas são criadas pela própria
 * API na subida ({@link #criarTabelas()}), sem migration manual.
 */
@Repository
public class ServidorRepository {

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
                modelo      VARCHAR(120) NULL,
                tipo        VARCHAR(10)  NULL,
                temperatura DOUBLE       NULL,
                estado      VARCHAR(12)  NULL,
                KEY ix_amostra_disco_coletado (coletado_em, nome)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    /** Colunas de temperatura acrescentadas depois; o ALTER é idempotente para tabelas já criadas. */
    private static final String COLUNAS_TEMPERATURA_DISCO = """
            ALTER TABLE amostra_disco
                ADD COLUMN IF NOT EXISTS modelo      VARCHAR(120) NULL,
                ADD COLUMN IF NOT EXISTS tipo        VARCHAR(10)  NULL,
                ADD COLUMN IF NOT EXISTS temperatura DOUBLE       NULL,
                ADD COLUMN IF NOT EXISTS estado      VARCHAR(12)  NULL
            """;

    /** Pressão, GPU, limite da CPU, latência de disco e erros de rede, acrescentados depois. */
    private static final String COLUNAS_SISTEMA = """
            ALTER TABLE amostra_servidor
                ADD COLUMN IF NOT EXISTS psi_cpu            DOUBLE NULL,
                ADD COLUMN IF NOT EXISTS psi_mem            DOUBLE NULL,
                ADD COLUMN IF NOT EXISTS psi_io             DOUBLE NULL,
                ADD COLUMN IF NOT EXISTS gpu_pct            DOUBLE NULL,
                ADD COLUMN IF NOT EXISTS cpu_limite_pct     DOUBLE NULL,
                ADD COLUMN IF NOT EXISTS disco_latencia_ms  DOUBLE NULL,
                ADD COLUMN IF NOT EXISTS disco_ocupacao_pct DOUBLE NULL,
                ADD COLUMN IF NOT EXISTS rede_erros         BIGINT NULL
            """;

    private static final String TABELA_HOST = """
            CREATE TABLE IF NOT EXISTS amostra_host (
                id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                coletado_em         DATETIME NOT NULL,
                internet_ok         BOOLEAN  NULL,
                internet_ms         DOUBLE   NULL,
                gateway_ms          DOUBLE   NULL,
                dns_ok              BOOLEAN  NULL,
                dns_ms              DOUBLE   NULL,
                tailscale_online    BOOLEAN  NULL,
                npu_pct             DOUBLE   NULL,
                alertas             INT      NOT NULL,
                pendentes           INT      NOT NULL,
                servicos_falhos     INT      NOT NULL,
                discos_desmontados  INT      NOT NULL,
                kernel_eventos_24h  INT      NULL,
                apt_pendentes       INT      NULL,
                apt_seguranca       INT      NULL,
                reinicio_necessario BOOLEAN  NULL,
                backup_idade_seg    BIGINT   NULL,
                backup_ok           BOOLEAN  NULL,
                containers_reinicios BIGINT  NULL,
                KEY ix_amostra_host_coletado (coletado_em)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String TABELA_EVENTO = """
            CREATE TABLE IF NOT EXISTS evento_host (
                id       BIGINT AUTO_INCREMENT PRIMARY KEY,
                quando   DATETIME     NOT NULL,
                tipo     VARCHAR(20)  NOT NULL,
                chave    VARCHAR(190) NOT NULL,
                nivel    VARCHAR(10)  NOT NULL,
                mensagem VARCHAR(500) NOT NULL,
                UNIQUE KEY ux_evento_host_chave (chave),
                KEY ix_evento_host_quando (quando)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String AGREGADOS_HOST = """
            COUNT(*) AS amostras,
            COALESCE(SUM(internet_ok = FALSE), 0) AS sem_internet,
            MIN(internet_ms) AS net_min, AVG(internet_ms) AS net_med, MAX(internet_ms) AS net_max,
            AVG(gateway_ms) AS gw_med,
            COALESCE(SUM(dns_ok = FALSE), 0) AS dns_falha, AVG(dns_ms) AS dns_med,
            COALESCE(SUM(tailscale_online = FALSE), 0) AS ts_off,
            AVG(npu_pct) AS npu_med, MAX(npu_pct) AS npu_max,
            MAX(alertas) AS alertas_max, COALESCE(SUM(alertas > 0), 0) AS min_alerta,
            MAX(servicos_falhos) AS serv_max, COALESCE(SUM(discos_desmontados > 0), 0) AS min_desm,
            MAX(backup_idade_seg) AS bkp_max,
            MAX(containers_reinicios) - MIN(containers_reinicios) AS reinicios
            """;

    private static final String SERIE_HOST = """
            SELECT HOUR(coletado_em) * 60 + FLOOR(MINUTE(coletado_em) / 5) * 5 AS minuto,
                   AVG(internet_ms) AS net, MAX(internet_ms) AS net_max, AVG(gateway_ms) AS gw, AVG(dns_ms) AS dns,
                   100 * AVG(internet_ok = FALSE) AS fora, AVG(npu_pct) AS npu
              FROM amostra_host
             WHERE coletado_em >= ? AND coletado_em < ?
             GROUP BY minuto
             ORDER BY minuto
            """;

    private static final String SERIE_DISCOS = """
            SELECT nome, HOUR(coletado_em) * 60 + FLOOR(MINUTE(coletado_em) / 5) * 5 AS minuto,
                   AVG(temperatura) AS temperatura
              FROM amostra_disco
             WHERE coletado_em >= ? AND coletado_em < ? AND temperatura IS NOT NULL
             GROUP BY nome, minuto
             ORDER BY nome, minuto
            """;

    private static final String TABELA_CONTAINER = """
            CREATE TABLE IF NOT EXISTS amostra_container (
                id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                coletado_em       DATETIME     NOT NULL,
                intervalo_seg     INT          NOT NULL,
                nome              VARCHAR(128) NOT NULL,
                imagem            VARCHAR(255) NOT NULL,
                estado            VARCHAR(20)  NOT NULL,
                saude             VARCHAR(20)  NULL,
                status            VARCHAR(160) NULL,
                cpu_pct           DOUBLE       NULL,
                mem_usada         BIGINT       NULL,
                mem_limite        BIGINT       NULL,
                rede_rx_bps       DOUBLE       NULL,
                rede_tx_bps       DOUBLE       NULL,
                disco_leitura_bps DOUBLE       NULL,
                disco_escrita_bps DOUBLE       NULL,
                processos         INT          NULL,
                KEY ix_amostra_container_coletado (coletado_em, nome)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

    private static final String INSERIR_CONTAINER = """
            INSERT INTO amostra_container (coletado_em, intervalo_seg, nome, imagem, estado, saude, status, cpu_pct,
                mem_usada, mem_limite, rede_rx_bps, rede_tx_bps, disco_leitura_bps, disco_escrita_bps, processos)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    /** Agregados de cada container no período, com imagem/estado/status da última amostra. */
    private static final String CONTAINERS_DO_DIA = """
            SELECT g.*, u.imagem, u.estado, u.status, u.saude
              FROM (SELECT nome, MAX(id) AS ultimo_id, COUNT(*) AS minutos,
                           SUM(estado = 'running') AS rodando,
                           AVG(cpu_pct) AS cpu_med, MAX(cpu_pct) AS cpu_max,
                           AVG(mem_usada) AS mem_med, MAX(mem_usada) AS mem_max,
                           SUM(rede_rx_bps * intervalo_seg) AS rede_rx, SUM(rede_tx_bps * intervalo_seg) AS rede_tx,
                           SUM(disco_leitura_bps * intervalo_seg) AS disco_lido,
                           SUM(disco_escrita_bps * intervalo_seg) AS disco_escrito
                      FROM amostra_container
                     WHERE coletado_em >= ? AND coletado_em < ?
                     GROUP BY nome) g
              JOIN amostra_container u ON u.id = g.ultimo_id
             ORDER BY g.cpu_med IS NULL, g.cpu_med DESC, g.nome
            """;

    private static final String SERIE_CONTAINERS = """
            SELECT nome, HOUR(coletado_em) * 60 + FLOOR(MINUTE(coletado_em) / 5) * 5 AS minuto,
                   AVG(cpu_pct) AS cpu, AVG(mem_usada) AS mem
              FROM amostra_container
             WHERE coletado_em >= ? AND coletado_em < ? AND estado = 'running'
             GROUP BY nome, minuto
             ORDER BY nome, minuto
            """;

    private static final String INSERIR = """
            INSERT INTO amostra_servidor (coletado_em, intervalo_seg, cpu_pct, iowait_pct, load1, load5, load15,
                processos, mem_total, mem_usada, mem_pct, swap_total, swap_usada, swap_pct,
                temp_max, temp_soc, temp_cpu, temp_gpu, rede_rx_bps, rede_tx_bps, disco_leitura_bps, disco_escrita_bps,
                psi_cpu, psi_mem, psi_io, gpu_pct, cpu_limite_pct, disco_latencia_ms, disco_ocupacao_pct, rede_erros)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            SUM(disco_leitura_bps * intervalo_seg) AS disco_lido, SUM(disco_escrita_bps * intervalo_seg) AS disco_escrito,
            AVG(psi_cpu) AS psi_cpu_med, MAX(psi_cpu) AS psi_cpu_max, AVG(psi_mem) AS psi_mem_med, MAX(psi_mem) AS psi_mem_max,
            AVG(psi_io) AS psi_io_med, MAX(psi_io) AS psi_io_max, AVG(gpu_pct) AS gpu_med, MAX(gpu_pct) AS gpu_max,
            MIN(cpu_limite_pct) AS cpu_limite_min, SUM(cpu_limite_pct < 99.5) AS minutos_limitada,
            AVG(disco_latencia_ms) AS lat_med, MAX(disco_latencia_ms) AS lat_max, MAX(disco_ocupacao_pct) AS ocup_max,
            SUM(rede_erros) AS rede_erros
            """;

    private static final String SERIE = """
            SELECT HOUR(coletado_em) * 60 + FLOOR(MINUTE(coletado_em) / 5) * 5 AS minuto,
                   AVG(cpu_pct) AS cpu, MAX(cpu_pct) AS cpu_max, AVG(mem_pct) AS mem, AVG(swap_pct) AS swap,
                   AVG(temp_max) AS temp, MAX(temp_max) AS temp_max, AVG(load1) AS carga, AVG(iowait_pct) AS iowait,
                   AVG(rede_rx_bps) AS rx, AVG(rede_tx_bps) AS tx,
                   AVG(disco_leitura_bps) AS leitura, AVG(disco_escrita_bps) AS escrita,
                   AVG(psi_cpu) AS psi_cpu, AVG(psi_mem) AS psi_mem, AVG(psi_io) AS psi_io, AVG(gpu_pct) AS gpu,
                   MIN(cpu_limite_pct) AS cpu_limite, AVG(disco_latencia_ms) AS lat, MAX(disco_latencia_ms) AS lat_max,
                   AVG(disco_ocupacao_pct) AS ocup, SUM(rede_erros) AS rede_erros
              FROM amostra_servidor
             WHERE coletado_em >= ? AND coletado_em < ?
             GROUP BY minuto
             ORDER BY minuto
            """;

    /** Última medição do dia de cada disco, com a temperatura média e máxima do dia. */
    private static final String DISCOS_DO_DIA = """
            SELECT d.nome, d.caminho, d.total, d.usado, d.pct, d.modelo, d.tipo, d.temperatura, d.estado,
                   u.temp_med, u.temp_max
              FROM amostra_disco d
              JOIN (SELECT nome, MAX(coletado_em) AS ultima, AVG(temperatura) AS temp_med, MAX(temperatura) AS temp_max
                      FROM amostra_disco
                     WHERE coletado_em >= ? AND coletado_em < ?
                     GROUP BY nome) u ON u.nome = d.nome AND u.ultima = d.coletado_em
             ORDER BY d.id
            """;

    private final JdbcTemplate jdbc;

    public ServidorRepository(@Qualifier(MonitorConfig.MONITOR) DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public void criarTabelas() {
        jdbc.execute(TABELA_AMOSTRA);
        jdbc.execute(COLUNAS_SISTEMA);
        jdbc.execute(TABELA_DISCO);
        jdbc.execute(COLUNAS_TEMPERATURA_DISCO);
        jdbc.execute(TABELA_CONTAINER);
        jdbc.execute(TABELA_HOST);
        jdbc.execute(TABELA_EVENTO);
    }

    // ---- host ----

    public void inserirHost(AmostraHost a) {
        jdbc.update("""
                        INSERT INTO amostra_host (coletado_em, internet_ok, internet_ms, gateway_ms, dns_ok, dns_ms,
                            tailscale_online, npu_pct, alertas, pendentes, servicos_falhos, discos_desmontados,
                            kernel_eventos_24h, apt_pendentes, apt_seguranca, reinicio_necessario, backup_idade_seg,
                            backup_ok, containers_reinicios)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                a.coletadoEm(), a.internetOk(), a.internetMs(), a.gatewayMs(), a.dnsOk(), a.dnsMs(), a.tailscaleOnline(),
                a.npuPct(), a.alertas(), a.pendentes(), a.servicosFalhos(), a.discosDesmontados(), a.kernelEventos24h(),
                a.aptPendentes(), a.aptSeguranca(), a.reinicioNecessario(), a.backupIdadeSeg(), a.backupOk(),
                a.containersReinicios());
    }

    /** Grava os eventos novos; os que já existem (mesma chave) são ignorados. Devolve quantos entraram. */
    public int inserirEventos(List<EventosHost.Evento> eventos) {
        if (eventos.isEmpty()) return 0;
        int[] r = jdbc.batchUpdate("INSERT IGNORE INTO evento_host (quando, tipo, chave, nivel, mensagem) VALUES (?, ?, ?, ?, ?)",
                eventos.stream().map(e -> new Object[]{e.quando(), corte(e.tipo(), 20), corte(e.chave(), 190),
                        corte(e.nivel(), 10), corte(e.mensagem(), 500)}).toList());
        int n = 0;
        for (int x : r) if (x > 0) n++;
        return n;
    }

    public List<ServidorEventoResponse> eventos(LocalDateTime de, LocalDateTime ate, int limite) {
        return jdbc.query("SELECT quando, tipo, nivel, mensagem FROM evento_host WHERE quando >= ? AND quando < ?"
                        + " ORDER BY quando DESC, id DESC LIMIT ?",
                (rs, i) -> new ServidorEventoResponse(rs.getObject("quando", LocalDateTime.class), rs.getString("tipo"),
                        rs.getString("nivel"), rs.getString("mensagem")), de, ate, limite);
    }

    public List<ServidorHostDiaResponse.Ponto> serieHost(LocalDateTime de, LocalDateTime ate) {
        return jdbc.query(SERIE_HOST, (rs, i) -> {
            int minuto = rs.getInt("minuto");
            return new ServidorHostDiaResponse.Ponto("%02d:%02d".formatted(minuto / 60, minuto % 60),
                    numero(rs, "net"), numero(rs, "net_max"), numero(rs, "gw"), numero(rs, "dns"),
                    rs.getDouble("fora"), numero(rs, "npu"));
        }, de, ate);
    }

    /** Resumo do host no período; nulo sem amostras. */
    public ServidorHostDiaResponse.Resumo resumoHost(LocalDateTime de, LocalDateTime ate) {
        ServidorHostDiaResponse.Resumo r = jdbc.queryForObject(
                "SELECT " + AGREGADOS_HOST + " FROM amostra_host WHERE coletado_em >= ? AND coletado_em < ?",
                (rs, i) -> resumoHost(rs), de, ate);
        if (r == null || r.amostras() == 0) return null;
        return completarHost(r, de, ate);
    }

    /** Resumo do host por dia do período. */
    public Map<LocalDate, ServidorHostDiaResponse.Resumo> resumoHostPorDia(LocalDateTime de, LocalDateTime ate) {
        Map<LocalDate, ServidorHostDiaResponse.Resumo> porDia = new LinkedHashMap<>();
        jdbc.query("SELECT DATE(coletado_em) AS dia, " + AGREGADOS_HOST
                        + " FROM amostra_host WHERE coletado_em >= ? AND coletado_em < ? GROUP BY DATE(coletado_em)",
                rs -> {
                    porDia.put(rs.getObject("dia", LocalDate.class), resumoHost(rs));
                }, de, ate);
        Map<LocalDate, long[]> eventos = new LinkedHashMap<>();
        jdbc.query("SELECT DATE(quando) AS dia, COUNT(*) AS n, SUM(nivel = 'erro') AS erros FROM evento_host"
                        + " WHERE quando >= ? AND quando < ? GROUP BY DATE(quando)",
                rs -> {
                    eventos.put(rs.getObject("dia", LocalDate.class), new long[]{rs.getLong("n"), rs.getLong("erros")});
                }, de, ate);
        porDia.replaceAll((dia, r) -> {
            long[] ev = eventos.getOrDefault(dia, new long[]{0, 0});
            return comEventos(r, ev[0], ev[1], null, null);
        });
        return porDia;
    }

    private ServidorHostDiaResponse.Resumo completarHost(ServidorHostDiaResponse.Resumo r, LocalDateTime de, LocalDateTime ate) {
        long[] ev = jdbc.queryForObject("SELECT COUNT(*) AS n, COALESCE(SUM(nivel = 'erro'), 0) AS erros FROM evento_host"
                + " WHERE quando >= ? AND quando < ?", (rs, i) -> new long[]{rs.getLong("n"), rs.getLong("erros")}, de, ate);
        record UltimaDoHost(Long aptPendentes, Boolean reinicioNecessario) {
        }
        UltimaDoHost u = jdbc.query("SELECT apt_pendentes, reinicio_necessario FROM amostra_host"
                        + " WHERE coletado_em >= ? AND coletado_em < ? ORDER BY coletado_em DESC LIMIT 1",
                (rs, i) -> new UltimaDoHost(inteiro(rs, "apt_pendentes"), logicoOuNulo(rs, "reinicio_necessario")), de, ate)
                .stream().findFirst().orElse(new UltimaDoHost(null, null));
        return comEventos(r, ev == null ? 0 : ev[0], ev == null ? 0 : ev[1], u.aptPendentes(), u.reinicioNecessario());
    }

    private static ServidorHostDiaResponse.Resumo comEventos(ServidorHostDiaResponse.Resumo r, long eventos, long erros,
                                                             Long apt, Boolean reboot) {
        return new ServidorHostDiaResponse.Resumo(r.amostras(), r.minutosSemInternet(), r.internetLatencia(),
                r.gatewayLatenciaMedia(), r.minutosDnsFalhando(), r.dnsTempoMedio(), r.minutosTailscaleOffline(), r.npu(),
                r.alertasMax(), r.minutosComAlerta(), r.servicosFalhosMax(), r.minutosDiscoDesmontado(),
                r.backupIdadeMaxHoras(), r.containersReinicios(), apt, reboot, eventos, erros);
    }

    private static ServidorHostDiaResponse.Resumo resumoHost(ResultSet rs) throws SQLException {
        Long bkp = inteiro(rs, "bkp_max");
        return new ServidorHostDiaResponse.Resumo(rs.getLong("amostras"), rs.getLong("sem_internet"),
                new Faixa(numero(rs, "net_min"), numero(rs, "net_med"), numero(rs, "net_max")), numero(rs, "gw_med"),
                rs.getLong("dns_falha"), numero(rs, "dns_med"), rs.getLong("ts_off"),
                new Faixa(null, numero(rs, "npu_med"), numero(rs, "npu_max")),
                rs.getLong("alertas_max"), rs.getLong("min_alerta"), rs.getLong("serv_max"), rs.getLong("min_desm"),
                bkp == null ? null : Math.round(bkp / 360.0) / 10.0, inteiro(rs, "reinicios"), null, null, 0, 0);
    }

    private static Boolean logicoOuNulo(ResultSet rs, String coluna) throws SQLException {
        boolean v = rs.getBoolean(coluna);
        return rs.wasNull() ? null : v;
    }

    public int apagarHostAntesDe(LocalDateTime limite) {
        return jdbc.update("DELETE FROM amostra_host WHERE coletado_em < ?", limite)
                + jdbc.update("DELETE FROM evento_host WHERE quando < ?", limite);
    }

    public void inserir(Amostra a) {
        jdbc.update(INSERIR, a.coletadoEm(), a.intervaloSeg(), a.cpuPct(), a.iowaitPct(), a.load1(), a.load5(),
                a.load15(), a.processos(), a.memTotal(), a.memUsada(), a.memPct(), a.swapTotal(), a.swapUsada(),
                a.swapPct(), a.tempMax(), a.tempSoc(), a.tempCpu(), a.tempGpu(), a.redeRxBps(), a.redeTxBps(),
                a.discoLeituraBps(), a.discoEscritaBps(), a.psiCpu(), a.psiMemoria(), a.psiIo(), a.gpuPct(),
                a.cpuLimitePct(), a.discoLatenciaMs(), a.discoOcupacaoPct(), a.redeErros());
    }

    public void inserirDiscos(LocalDateTime coletadoEm, List<LeitorSistema.EspacoDisco> discos) {
        if (discos.isEmpty()) return;
        jdbc.batchUpdate("""
                        INSERT INTO amostra_disco (coletado_em, nome, caminho, total, usado, pct, modelo, tipo, temperatura, estado)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                discos.stream().map(d -> new Object[]{coletadoEm, d.nome(), d.caminho(), d.total(), d.usado(), d.pct(),
                        corte(d.modelo(), 120), corte(d.tipo(), 10), d.temperatura(), corte(d.estadoTemperatura(), 12)}).toList());
    }

    public void inserirContainers(List<AmostraContainer> amostras) {
        if (amostras.isEmpty()) return;
        jdbc.batchUpdate(INSERIR_CONTAINER, amostras.stream().map(a -> new Object[]{a.coletadoEm(), a.intervaloSeg(),
                corte(a.nome(), 128), corte(a.imagem(), 255), corte(a.estado(), 20), a.saude(), corte(a.status(), 160),
                a.cpuPct(), a.memUsada(), a.memLimite(), a.redeRxBps(), a.redeTxBps(), a.discoLeituraBps(),
                a.discoEscritaBps(), a.processos()}).toList());
    }

    public List<ServidorDiaResponse.ContainerDia> containers(LocalDateTime de, LocalDateTime ate) {
        return jdbc.query(CONTAINERS_DO_DIA, (rs, i) -> new ServidorDiaResponse.ContainerDia(
                rs.getString("nome"), rs.getString("imagem"), rs.getString("estado"), rs.getString("status"),
                rs.getString("saude"), rs.getLong("minutos"), rs.getLong("rodando"),
                numero(rs, "cpu_med"), numero(rs, "cpu_max"), inteiro(rs, "mem_med"), inteiro(rs, "mem_max"),
                inteiro(rs, "rede_rx"), inteiro(rs, "rede_tx"), inteiro(rs, "disco_lido"), inteiro(rs, "disco_escrito")),
                de, ate);
    }

    public List<ServidorDiaResponse.SerieContainer> serieContainers(LocalDateTime de, LocalDateTime ate) {
        Map<String, List<ServidorDiaResponse.PontoContainer>> porNome = new LinkedHashMap<>();
        jdbc.query(SERIE_CONTAINERS, rs -> {
            int minuto = rs.getInt("minuto");
            porNome.computeIfAbsent(rs.getString("nome"), n -> new ArrayList<>())
                    .add(new ServidorDiaResponse.PontoContainer("%02d:%02d".formatted(minuto / 60, minuto % 60),
                            numero(rs, "cpu"), inteiro(rs, "mem")));
        }, de, ate);
        return porNome.entrySet().stream()
                .map(e -> new ServidorDiaResponse.SerieContainer(e.getKey(), e.getValue())).toList();
    }

    public int apagarContainersAntesDe(LocalDateTime limite) {
        return jdbc.update("DELETE FROM amostra_container WHERE coletado_em < ?", limite);
    }

    private static String corte(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    public ServidorEstatisticasResponse resumo(LocalDateTime de, LocalDateTime ate) {
        ServidorEstatisticasResponse r = jdbc.queryForObject(
                "SELECT " + AGREGADOS + " FROM amostra_servidor WHERE coletado_em >= ? AND coletado_em < ?",
                (rs, i) -> estatisticas(rs), de, ate);
        return r.comDiscoTemperaturaMax(temperaturaMaxDiscosPorDia(de, ate).values().stream()
                .max(Double::compare).orElse(null));
    }

    public List<ServidorDiaResponse.Ponto> serie(LocalDateTime de, LocalDateTime ate) {
        return jdbc.query(SERIE, (rs, i) -> {
            int minuto = rs.getInt("minuto");
            return new ServidorDiaResponse.Ponto(
                    "%02d:%02d".formatted(minuto / 60, minuto % 60),
                    rs.getDouble("cpu"), rs.getDouble("cpu_max"), rs.getDouble("mem"), rs.getDouble("swap"),
                    numero(rs, "temp"), numero(rs, "temp_max"), rs.getDouble("carga"), rs.getDouble("iowait"),
                    rs.getDouble("rx"), rs.getDouble("tx"), rs.getDouble("leitura"), rs.getDouble("escrita"),
                    numero(rs, "psi_cpu"), numero(rs, "psi_mem"), numero(rs, "psi_io"), numero(rs, "gpu"),
                    numero(rs, "cpu_limite"), numero(rs, "lat"), numero(rs, "lat_max"), numero(rs, "ocup"),
                    inteiro(rs, "rede_erros"));
        }, de, ate);
    }

    public List<ServidorDiscoResponse> discos(LocalDateTime de, LocalDateTime ate) {
        return jdbc.query(DISCOS_DO_DIA, (rs, i) -> new ServidorDiscoResponse(rs.getString("nome"), rs.getString("caminho"),
                rs.getLong("total"), rs.getLong("usado"), rs.getDouble("pct"), rs.getString("modelo"), rs.getString("tipo"),
                numero(rs, "temperatura"), rs.getString("estado"), numero(rs, "temp_med"), numero(rs, "temp_max"), null), de, ate);
    }

    public List<ServidorDiaResponse.SerieDisco> serieDiscos(LocalDateTime de, LocalDateTime ate) {
        Map<String, List<ServidorDiaResponse.PontoDisco>> porNome = new LinkedHashMap<>();
        jdbc.query(SERIE_DISCOS, rs -> {
            int minuto = rs.getInt("minuto");
            porNome.computeIfAbsent(rs.getString("nome"), n -> new ArrayList<>())
                    .add(new ServidorDiaResponse.PontoDisco("%02d:%02d".formatted(minuto / 60, minuto % 60),
                            numero(rs, "temperatura")));
        }, de, ate);
        return porNome.entrySet().stream().map(e -> new ServidorDiaResponse.SerieDisco(e.getKey(), e.getValue())).toList();
    }

    /** Maior temperatura entre os discos em cada dia do período. */
    public Map<LocalDate, Double> temperaturaMaxDiscosPorDia(LocalDateTime de, LocalDateTime ate) {
        Map<LocalDate, Double> porDia = new LinkedHashMap<>();
        jdbc.query("SELECT DATE(coletado_em) AS dia, MAX(temperatura) AS t FROM amostra_disco"
                        + " WHERE coletado_em >= ? AND coletado_em < ? GROUP BY DATE(coletado_em)",
                rs -> {
                    Double t = numero(rs, "t");
                    if (t != null) porDia.put(rs.getObject("dia", LocalDate.class), t);
                }, de, ate);
        return porDia;
    }

    /** Um item por dia com amostras, do mais recente para o mais antigo. */
    public List<ServidorResumoDiaResponse> porDia(LocalDate de, LocalDate ate) {
        LocalDateTime inicio = de.atStartOfDay();
        LocalDateTime fim = ate.plusDays(1).atStartOfDay();
        Map<LocalDate, Double> discos = temperaturaMaxDiscosPorDia(inicio, fim);
        Map<LocalDate, ServidorHostDiaResponse.Resumo> hosts = resumoHostPorDia(inicio, fim);
        return jdbc.query("SELECT DATE(coletado_em) AS dia, " + AGREGADOS
                        + " FROM amostra_servidor WHERE coletado_em >= ? AND coletado_em < ?"
                        + " GROUP BY DATE(coletado_em) ORDER BY dia DESC",
                (rs, i) -> {
                    LocalDate dia = rs.getObject("dia", LocalDate.class);
                    return new ServidorResumoDiaResponse(dia, estatisticas(rs).comDiscoTemperaturaMax(discos.get(dia)),
                            hosts.get(dia));
                }, inicio, fim);
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
                inteiro(rs, "rede_rx"), inteiro(rs, "rede_tx"), inteiro(rs, "disco_lido"), inteiro(rs, "disco_escrito"),
                null,
                new Faixa(null, numero(rs, "psi_cpu_med"), numero(rs, "psi_cpu_max")),
                new Faixa(null, numero(rs, "psi_mem_med"), numero(rs, "psi_mem_max")),
                new Faixa(null, numero(rs, "psi_io_med"), numero(rs, "psi_io_max")),
                new Faixa(null, numero(rs, "gpu_med"), numero(rs, "gpu_max")),
                numero(rs, "cpu_limite_min"), inteiro(rs, "minutos_limitada"),
                new Faixa(null, numero(rs, "lat_med"), numero(rs, "lat_max")),
                numero(rs, "ocup_max"), inteiro(rs, "rede_erros"));
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
