package com.kowalski.casaapi.servidor;

import com.kowalski.casaapi.config.monitor.MonitorProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Métricas novas do sistema (PSI, GPU, limite da CPU, latência de disco) e status do host. */
class MonitorHostTest {

    @TempDir Path raiz;

    // ---- sistema ----

    @Test
    void lePressaoGpuELimiteDaCpu() throws IOException {
        Path proc = raiz.resolve("proc");
        Path sys = raiz.resolve("sys");
        escrever(proc.resolve("pressure/cpu"), "some avg10=1.50 avg60=0.80 avg300=0.10 total=1\nfull avg10=0.00 avg60=0.00 avg300=0.00 total=0\n");
        escrever(proc.resolve("pressure/memory"), "some avg10=0.25 avg60=0 avg300=0 total=1\nfull avg10=0.10 avg60=0 avg300=0 total=1\n");
        escrever(proc.resolve("pressure/io"), "some avg10=3.00 avg60=0 avg300=0 total=1\nfull avg10=2.00 avg60=0 avg300=0 total=1\n");
        escrever(sys.resolve("class/devfreq/fb000000.gpu/load"), "37@800000000Hz\n");
        escrever(sys.resolve("class/devfreq/dmc/load"), "100@2112000000Hz\n");
        cluster(sys, "policy0", "0 1 2 3", 1800000, 1800000, 1800000);
        cluster(sys, "policy4", "4 5", 1200000, 2400000, 1800000);
        escrever(proc.resolve("stat"), "cpu 1 0 1 1 0 0 0 0\nbtime 1794700000\n");
        escrever(proc.resolve("meminfo"), "MemTotal: 100 kB\nMemAvailable: 50 kB\nSwapTotal: 0 kB\nSwapFree: 0 kB\n");
        escrever(proc.resolve("loadavg"), "0.1 0.1 0.1 1/10 1\n");
        escrever(proc.resolve("uptime"), "10.0 10.0\n");

        var leitor = new LeitorSistema(new MonitorProperties(true, 365, proc.toString(), sys.toString(),
                raiz.resolve("hostname").toString(), List.of(), null, null, null, null, null), java.time.Clock.systemDefaultZone());
        var inst = leitor.instantaneo();

        assertThat(inst.pressao()).isEqualTo(new LeitorSistema.Pressao(1.5, 0.25, 0.10, 3.0, 2.0));
        assertThat(inst.gpuPct()).isEqualTo(37.0);
        assertThat(inst.clusters()).hasSize(2);
        assertThat(inst.clusters().get(1).nucleos()).containsExactly(4, 5);
        assertThat(inst.clusters().get(1).limitePct()).isEqualTo(75.0);
        assertThat(inst.cpuLimitePct()).isEqualTo(75.0);
        assertThat(leitor.bootEpoch()).isEqualTo(1794700000L);
    }

    @Test
    void semPsiNemGpuFicamNulos() {
        assertThat(LeitorSistema.parsePressao(List.of(), "some")).isNull();
        assertThat(LeitorSistema.parseCargaDevfreq(null)).isNull();
        assertThat(LeitorSistema.parseCargaDevfreq("lixo")).isNull();
        assertThat(LeitorSistema.parseCargaDevfreq("12")).isEqualTo(12.0);
    }

    @Test
    void latenciaEOcupacaoDosDiscos() {
        var antes = LeitorSistema.parseDiskstatsPorDisco(List.of(
                "8 48 sdd 100 0 800 1000 50 0 400 500 0 2000 1500 0 0 0 0",
                "8 49 sdd1 100 0 800 1000 50 0 400 500 0 2000 1500 0 0 0 0"));
        // +30 leituras (300 ms) e +10 escritas (100 ms) = 40 ops, 400 ms; ocupado +250 ms em 1 s
        var depois = LeitorSistema.parseDiskstatsPorDisco(List.of(
                "8 48 sdd 130 0 900 1300 60 0 500 600 0 2250 1900 0 0 0 0"));
        assertThat(antes).containsOnlyKeys("sdd");

        var a = new LeitorSistema.Contadores(0, new LeitorSistema.CpuTempos(1, 1, 0), List.of(), Map.of(), 0, 0, antes, 10);
        var d = new LeitorSistema.Contadores(1_000_000_000L, new LeitorSistema.CpuTempos(2, 2, 0), List.of(), Map.of(), 0, 0, depois, 7);

        assertThat(d.latenciaDiscosMs(a)).isCloseTo(10.0, within(0.001));
        assertThat(d.ocupacaoMaxDiscosPct(a)).isCloseTo(25.0, within(0.001));
        assertThat(d.atividadeDiscos(a)).singleElement().satisfies(x -> assertThat(x.disco()).isEqualTo("sdd"));
        assertThat(d.redeErrosDesde(a)).isZero(); // contador zerou: não vira negativo
        assertThat(a.latenciaDiscosMs(a)).isNull(); // sem operações
    }

    // ---- host ----

    private Map<String, Object> host(String json) {
        @SuppressWarnings("unchecked")
        Map<String, Object> m = JsonMapper.shared().readValue(json, Map.class);
        return m;
    }

    private static final String HOST = """
            {"versao":1,"lidoEm":"%s",
             "backup":{"repositorios":[{"nome":"HD Mídia","montado":true,"idadeSegundos":3600},
                                       {"nome":"OneDrive","montado":null,"idadeSegundos":7200}],
                       "ultimaExecucao":{"inicio":"2026-11-15T03:35:22-03:00","fim":"2026-11-15T03:41:30-03:00",
                                         "rodandoAgora":false,"sucesso":true,"falhas":0}},
             "servicos":[{"unidade":"docker.service","instalado":true,"ok":true},
                         {"unidade":"x.service","instalado":false,"ok":false}],
             "unidadesFalhas":[],
             "montagens":{"discos":[{"ponto":"/mnt/media","montado":true,"somenteLeitura":false}]},
             "alertas":{"alertados":%s,"pendentes":[]},
             "kernel":{"eventos24h":1,"ultimos":["2026-11-15T05:36:12-03:00 orangepi5plus kernel: usb 2-1: USB disconnect, device number 2"]},
             "atualizacoes":{"reinicioNecessario":false,"pacotesPendentes":2,"pacotesSeguranca":1},
             "rede":{"internet":{"online":%s,"latenciaMs":4.0},"gateway":{"latenciaMs":1.0},
                     "dns":{"ok":true,"tempoMs":30},"tailscale":{"online":true}},
             "loginsSsh":[{"quando":"2026-11-15T14:00:21-03:00","usuario":"root","origem":"192.168.68.124","metodo":"publickey"}],
             "npu":{"nucleosPct":[0,30,60]},
             "docker":{"containers":[{"nome":"jellyfin","reinicios":%d,"oomKilled":false}]}}
            """;

    private Map<String, Object> host(String alertas, boolean internet, int reinicios) {
        return host(HOST.formatted(Instant.now().toString(), alertas, internet, reinicios));
    }

    @Test
    void amostraDoHost() {
        AmostraHost a = AmostraHost.de(LocalDateTime.of(2026, 11, 15, 14, 0), host("[\"backup atrasado\"]", true, 3));

        assertThat(a.internetOk()).isTrue();
        assertThat(a.internetMs()).isEqualTo(4.0);
        assertThat(a.npuPct()).isEqualTo(30.0);
        assertThat(a.alertas()).isEqualTo(1);
        assertThat(a.servicosFalhos()).isZero(); // x.service não está instalado
        assertThat(a.backupIdadeSeg()).isEqualTo(7200L); // a cópia mais atrasada
        assertThat(a.aptSeguranca()).isEqualTo(1L);
        assertThat(a.containersReinicios()).isEqualTo(3L);
    }

    @Test
    void eventosComDataPropriaTemChaveEstavel() {
        var agora = LocalDateTime.of(2026, 11, 15, 14, 1);
        var h = host("[]", true, 0);

        var primeira = EventosHost.calcular(agora, h, new EventosHost.Estado());
        var segunda = EventosHost.calcular(agora.plusMinutes(1), h, new EventosHost.Estado());

        assertThat(primeira).extracting(EventosHost.Evento::tipo).containsExactlyInAnyOrder("ssh", "backup", "kernel");
        assertThat(primeira).extracting(EventosHost.Evento::chave)
                .containsExactlyInAnyOrderElementsOf(segunda.stream().map(EventosHost.Evento::chave).toList());
        assertThat(primeira).filteredOn(e -> e.tipo().equals("backup")).singleElement()
                .satisfies(e -> assertThat(e.mensagem()).isEqualTo("Backup concluído em 6 min"));
        assertThat(primeira).filteredOn(e -> e.tipo().equals("kernel")).singleElement()
                .satisfies(e -> assertThat(e.mensagem()).startsWith("Kernel: usb 2-1: USB disconnect"));
    }

    @Test
    void mudancasDeEstadoComparamComALeituraAnterior() {
        var estado = new EventosHost.Estado();
        var t = LocalDateTime.of(2026, 11, 15, 14, 0);

        // primeira leitura só serve de base
        assertThat(tipos(EventosHost.calcular(t, host("[\"disco cheio\"]", true, 0), estado))).doesNotContain("alerta");

        var e2 = EventosHost.calcular(t.plusMinutes(1), host("[]", false, 1), estado);
        assertThat(e2).filteredOn(e -> e.tipo().equals("alerta")).singleElement()
                .satisfies(e -> assertThat(e.mensagem()).isEqualTo("Resolvido: disco cheio"));
        assertThat(tipos(e2)).contains("container").doesNotContain("internet"); // uma leitura sem internet não é queda

        var e3 = EventosHost.calcular(t.plusMinutes(2), host("[]", false, 1), estado);
        assertThat(e3).filteredOn(e -> e.tipo().equals("internet")).singleElement()
                .satisfies(e -> assertThat(e.mensagem()).isEqualTo("Internet fora do ar"));

        var e4 = EventosHost.calcular(t.plusMinutes(5), host("[]", true, 1), estado);
        assertThat(e4).filteredOn(e -> e.tipo().equals("internet")).singleElement()
                .satisfies(e -> assertThat(e.mensagem()).isEqualTo("Internet voltou (fora por ~4 min)"));
    }

    private Map<String, Object> vpn(long ipadRx, String ipadHandshake, String dono) {
        return host("""
                {"rede":{"tailscale":{"online":true,"usuario":"eu@casa","aparelhos":[
                  {"id":"n1","nome":"ipad157","so":"iOS","ip":"100.71.41.49","dono":"%s","online":true,
                   "ultimoHandshake":%s,"recebidos":%d,"enviados":%d,"criado":"2025-01-01T00:00:00Z"},
                  {"id":"n2","nome":"desktop","so":"windows","ip":"100.109.174.99","dono":"eu@casa","online":false,
                   "ultimoHandshake":null,"recebidos":0,"enviados":0,"criado":"2025-01-01T00:00:00Z"}]}}}
                """.formatted(dono, ipadHandshake == null ? "null" : "\"" + ipadHandshake + "\"", ipadRx, ipadRx / 2));
    }

    private static List<String> mensagens(List<EventosHost.Evento> eventos) {
        return eventos.stream().filter(e -> e.tipo().equals("tailscale")).map(EventosHost.Evento::mensagem).toList();
    }

    @Test
    void acessoPelaVpnComecaComTrafegoETerminaDepoisDeOcioso() {
        var estado = new EventosHost.Estado();
        var t = LocalDateTime.of(2026, 11, 16, 15, 0);

        assertThat(mensagens(EventosHost.calcular(t, vpn(0, null, "eu@casa"), estado))).isEmpty(); // base

        // tráfego novo com o servidor: começou a acessar (online sozinho não conta: o desktop não aparece)
        assertThat(mensagens(EventosHost.calcular(t.plusMinutes(1), vpn(2048, "2026-11-16T18:01:00Z", "eu@casa"), estado)))
                .containsExactly("ipad157 (iOS · 100.71.41.49) começou a acessar o servidor pela VPN");
        // continua acessando: nada novo
        assertThat(mensagens(EventosHost.calcular(t.plusMinutes(3), vpn(1024 * 1024 * 3, "2026-11-16T18:01:00Z", "eu@casa"), estado)))
                .isEmpty();
        // parado há menos de 5 min: ainda conta como acesso
        assertThat(mensagens(EventosHost.calcular(t.plusMinutes(7), vpn(1024 * 1024 * 3, "2026-11-16T18:01:00Z", "eu@casa"), estado)))
                .isEmpty();
        var fim = EventosHost.calcular(t.plusMinutes(8), vpn(1024 * 1024 * 3, "2026-11-16T18:01:00Z", "eu@casa"), estado);
        assertThat(mensagens(fim)).containsExactly(
                "ipad157 (iOS · 100.71.41.49) parou de acessar o servidor pela VPN (2 min, 4,5 MB trocados)");

        // contador zerado pelo Tailscale e tráfego de novo: novo acesso; aparelho de outra conta mostra o dono
        assertThat(mensagens(EventosHost.calcular(t.plusMinutes(20), vpn(100, "2026-11-16T18:20:00Z", "visita@x"), estado)))
                .containsExactly("ipad157 (iOS · 100.71.41.49 · de visita@x) começou a acessar o servidor pela VPN");
    }

    @Test
    void acessoEmAndamentoQuandoAApiSobeNaoTemInicio() {
        var estado = new EventosHost.Estado();
        var t = LocalDateTime.of(2026, 11, 16, 15, 0);
        String agoraUtc = t.atZone(java.time.ZoneId.systemDefault()).toInstant().toString();

        assertThat(mensagens(EventosHost.calcular(t, vpn(500, agoraUtc, "eu@casa"), estado))).isEmpty();
        assertThat(mensagens(EventosHost.calcular(t.plusMinutes(6), vpn(500, agoraUtc, "eu@casa"), estado)))
                .containsExactly("ipad157 (iOS · 100.71.41.49) parou de acessar o servidor pela VPN (0 B trocados desde que a API subiu)");
    }

    @Test
    void aparelhoNovoNaTailnetTemChaveEstavel() {
        var t = LocalDateTime.of(2026, 11, 16, 15, 0);
        var h = host("""
                {"rede":{"tailscale":{"usuario":"eu@casa","aparelhos":[
                  {"id":"n9","nome":"celular","so":"android","ip":"100.1.2.3","dono":"eu@casa","criado":"2026-11-15T12:00:00Z"}]}}}
                """);
        var a = EventosHost.calcular(t, h, new EventosHost.Estado());
        var b = EventosHost.calcular(t.plusMinutes(1), h, new EventosHost.Estado());
        assertThat(a).filteredOn(e -> e.tipo().equals("tailscale")).singleElement().satisfies(e -> {
            assertThat(e.mensagem()).isEqualTo("Aparelho novo na rede Tailscale: celular (android · 100.1.2.3)");
            assertThat(e.nivel()).isEqualTo(EventosHost.ATENCAO);
        });
        assertThat(a).extracting(EventosHost.Evento::chave).isEqualTo(b.stream().map(EventosHost.Evento::chave).toList());
        // criado há mais de 7 dias: não é novo
        assertThat(mensagens(EventosHost.calcular(t.plusDays(8), h, new EventosHost.Estado()))).isEmpty();
    }

    @Test
    void arquivoVelhoEstaDesatualizado() {
        var h = host(HOST.formatted("2026-11-15T10:00:00-03:00", "[]", true, 0));
        assertThat(LeitorHost.desatualizado(h, Instant.parse("2026-11-15T13:04:00Z"))).isFalse();
        assertThat(LeitorHost.desatualizado(h, Instant.parse("2026-11-15T13:06:00Z"))).isTrue();
        assertThat(LeitorHost.desatualizado(Map.of(), Instant.now())).isTrue();
    }

    private static List<String> tipos(List<EventosHost.Evento> eventos) {
        return eventos.stream().map(EventosHost.Evento::tipo).toList();
    }

    private static void cluster(Path sys, String nome, String nucleos, long atual, long max, long limite) throws IOException {
        Path p = sys.resolve("devices/system/cpu/cpufreq/" + nome);
        escrever(p.resolve("related_cpus"), nucleos + "\n");
        escrever(p.resolve("scaling_cur_freq"), atual + "\n");
        escrever(p.resolve("cpuinfo_max_freq"), max + "\n");
        escrever(p.resolve("scaling_max_freq"), limite + "\n");
    }

    private static void escrever(Path p, String conteudo) throws IOException {
        Files.createDirectories(p.getParent());
        Files.writeString(p, conteudo);
    }
}
