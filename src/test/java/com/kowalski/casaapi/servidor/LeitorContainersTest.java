package com.kowalski.casaapi.servidor;

import com.kowalski.casaapi.config.monitor.MonitorProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LeitorContainersTest {
    private static Map<String, Object> stats(long cpu, long sistema, Map<String, Object> redes) {
        return Map.of(
                "cpu_stats", Map.of("cpu_usage", Map.of("total_usage", cpu), "system_cpu_usage", sistema, "online_cpus", 8),
                "memory_stats", Map.of("usage", 532_889_600L, "limit", 16_482_402_304L,
                        "stats", Map.of("inactive_file", 135_168L)),
                "blkio_stats", Map.of("io_service_bytes_recursive", List.of(
                        Map.of("major", 259, "minor", 0, "op", "read", "value", 13_516_800L),
                        Map.of("major", 259, "minor", 0, "op", "write", "value", 1_253_376L),
                        Map.of("major", 8, "minor", 0, "op", "write", "value", 1_000L))),
                "pids_stats", Map.of("current", 51),
                "networks", redes == null ? "" : redes);
    }

    @Test
    void leEstatisticasDoDocker() {
        var c = LeitorContainers.parseStats(stats(89_778_462_000L, 1_241_040_160_000_000L, Map.of(
                "eth0", Map.of("rx_bytes", 46_175, "tx_bytes", 53_970),
                "eth1", Map.of("rx_bytes", 8_784, "tx_bytes", 22_498))));

        assertThat(c.cpuNs()).isEqualTo(89_778_462_000L);
        assertThat(c.sistemaNs()).isEqualTo(1_241_040_160_000_000L);
        assertThat(c.memUsada()).isEqualTo(532_889_600L - 135_168L);
        assertThat(c.memLimite()).isEqualTo(16_482_402_304L);
        assertThat(c.redeRecebidos()).isEqualTo(46_175L + 8_784L);
        assertThat(c.redeEnviados()).isEqualTo(53_970L + 22_498L);
        assertThat(c.discoLidos()).isEqualTo(13_516_800L);
        assertThat(c.discoEscritos()).isEqualTo(1_253_376L + 1_000L);
        assertThat(c.processos()).isEqualTo(51);
    }

    @Test
    void containerNaRedeDoHostNaoTemRede() {
        var c = LeitorContainers.parseStats(stats(1, 2, null));
        assertThat(c.redeRecebidos()).isNull();
        assertThat(c.redeEnviados()).isNull();
    }

    @Test
    void semCpuNaoHaEstatistica() {
        assertThat(LeitorContainers.parseStats(Map.of("memory_stats", Map.of()))).isNull();
        assertThat(LeitorContainers.parseStats(null)).isNull();
    }

    @Test
    void saudeDoHealthcheck() {
        assertThat(LeitorContainers.saude("Up 13 minutes (healthy)")).isEqualTo("saudavel");
        assertThat(LeitorContainers.saude("Up 2 hours (unhealthy)")).isEqualTo("doente");
        assertThat(LeitorContainers.saude("Up 5 seconds (health: starting)")).isEqualTo("iniciando");
        assertThat(LeitorContainers.saude("Up 44 hours")).isNull();
    }

    @Test
    void traduzOStatus() {
        assertThat(LeitorContainers.traduzirStatus("Up 44 hours (healthy)")).isEqualTo("ligado há 44 horas");
        assertThat(LeitorContainers.traduzirStatus("Up About an hour")).isEqualTo("ligado há cerca de 1 hora");
        assertThat(LeitorContainers.traduzirStatus("Up Less than a second")).isEqualTo("ligado há menos de 1 segundo");
        assertThat(LeitorContainers.traduzirStatus("Up 3 days")).isEqualTo("ligado há 3 dias");
        assertThat(LeitorContainers.traduzirStatus("Exited (0) 3 days ago")).isEqualTo("parado (código 0) há 3 dias");
        assertThat(LeitorContainers.traduzirStatus("Exited (137) About a minute ago"))
                .isEqualTo("parado (código 137) há cerca de 1 minuto");
        assertThat(LeitorContainers.traduzirStatus("Created")).isEqualTo("criado");
        assertThat(LeitorContainers.traduzirStatus("Up 2 weeks (Paused)")).isEqualTo("ligado há 2 semanas (pausado)");
    }

    @Test
    void desligadoSemUrl() {
        var leitor = new LeitorContainers(new MonitorProperties(null, null, null, null, null, null, null, null, null, null, null));
        assertThat(leitor.habilitado()).isFalse();
        assertThat(leitor.ler().containers()).isEmpty();
    }

    private static LeitorContainers.Container rodando(String id, String nome, long cpu, long sistema, long rx, long escritos) {
        return new LeitorContainers.Container(id, nome, "img/" + nome, "running", "Up 2 hours", null,
                new LeitorContainers.Contadores(cpu, sistema, 100L, 1_000L, rx, rx, 0L, escritos, 10));
    }

    @Test
    void amostraCalculaCpuDoServidorETaxas() {
        var antes = new LeitorContainers.Leitura(0, List.of(
                rodando("a", "jellyfin", 1_000, 100_000, 0, 0),
                rodando("b", "reiniciado", 9_000, 100_000, 5_000, 0),
                new LeitorContainers.Container("c", "parado", "img", "exited", "Exited (0) 3 days ago", null, null)));
        var depois = new LeitorContainers.Leitura(60_000_000_000L, List.of(
                rodando("a", "jellyfin", 11_000, 200_000, 60_000, 120_000),
                rodando("b", "reiniciado", 500, 200_000, 100, 0),
                rodando("d", "novo", 50, 200_000, 10, 0),
                new LeitorContainers.Container("c", "parado", "img", "exited", "Exited (0) 3 days ago", null, null)));

        var amostras = AmostraContainer.de(LocalDateTime.of(2026, 11, 15, 12, 0), antes, depois);

        assertThat(amostras).extracting(AmostraContainer::nome).containsExactly("jellyfin", "reiniciado", "novo", "parado");
        var jellyfin = amostras.get(0);
        assertThat(jellyfin.intervaloSeg()).isEqualTo(60);
        assertThat(jellyfin.cpuPct()).isCloseTo(10.0, within(0.001));
        assertThat(jellyfin.redeRxBps()).isCloseTo(1_000, within(0.001));
        assertThat(jellyfin.discoEscritaBps()).isCloseTo(2_000, within(0.001));
        assertThat(jellyfin.memPct()).isEqualTo(10.0);
        assertThat(jellyfin.status()).isEqualTo("ligado há 2 horas");

        var reiniciado = amostras.get(1);
        assertThat(reiniciado.cpuPct()).isNull();
        assertThat(reiniciado.redeRxBps()).isNull();
        assertThat(reiniciado.memUsada()).isEqualTo(100L);

        assertThat(amostras.get(2).cpuPct()).isNull();
        var parado = amostras.get(3);
        assertThat(parado.estado()).isEqualTo("exited");
        assertThat(parado.cpuPct()).isNull();
        assertThat(parado.memUsada()).isNull();
        assertThat(parado.status()).isEqualTo("parado (código 0) há 3 dias");
    }

    @Test
    void agoraMostraRodandoPrimeiroPorCpu() {
        var antes = new LeitorContainers.Leitura(0, List.of(
                rodando("a", "pouco", 0, 0, 0, 0),
                rodando("b", "muito", 0, 0, 0, 0),
                new LeitorContainers.Container("c", "abc-parado", "img", "exited", "Exited (1) 2 hours ago", null, null)));
        var depois = new LeitorContainers.Leitura(500_000_000L, List.of(
                rodando("a", "pouco", 1_000, 100_000, 0, 0),
                rodando("b", "muito", 50_000, 100_000, 0, 0),
                new LeitorContainers.Container("c", "abc-parado", "img", "exited", "Exited (1) 2 hours ago", null, null)));

        var lista = ServidorService.containersAgora(java.time.LocalDateTime.now(), antes, depois);

        assertThat(lista).extracting(ServidorContainerResponse::nome).containsExactly("muito", "pouco", "abc-parado");
        assertThat(lista.get(0).cpuPct()).isEqualTo(50.0);
        assertThat(lista.get(1).cpuPct()).isEqualTo(1.0);
    }
}
