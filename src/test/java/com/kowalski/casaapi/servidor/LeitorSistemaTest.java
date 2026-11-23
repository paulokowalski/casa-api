package com.kowalski.casaapi.servidor;

import com.kowalski.casaapi.config.monitor.MonitorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LeitorSistemaTest {

    @TempDir Path raiz;
    Path proc;
    Path sys;

    @BeforeEach
    void sistemaFalso() throws IOException {
        proc = raiz.resolve("proc");
        sys = raiz.resolve("sys");
        escrever(proc.resolve("stat"), """
                cpu  1000 0 500 8000 500 0 0 0 0 0
                cpu0 500 0 250 4000 250 0 0 0 0 0
                cpu1 500 0 250 4000 250 0 0 0 0 0
                intr 123
                """);
        escrever(proc.resolve("meminfo"), """
                MemTotal:       16000000 kB
                MemFree:          600000 kB
                MemAvailable:   12000000 kB
                SwapTotal:       8000000 kB
                SwapFree:        6000000 kB
                """);
        escrever(proc.resolve("loadavg"), "1.11 1.60 1.83 2/1070 12\n");
        escrever(proc.resolve("uptime"), "156239.48 1120722.80\n");
        escrever(proc.resolve("diskstats"), """
                 179       0 mmcblk1 10 0 100 0 5 0 200 0 0 0 0 0 0 0 0 0 0
                 179       1 mmcblk1p1 10 0 999 0 5 0 999 0 0 0 0 0 0 0 0 0 0
                 259       0 nvme0n1 10 0 1000 0 5 0 2000 0 0 0 0 0 0 0 0 0 0
                 254       0 zram0 10 0 5000 0 5 0 5000 0 0 0 0 0 0 0 0 0 0
                """);
        escrever(proc.resolve("device-tree/model"), "Orange Pi 5 Plus\0");
        escrever(proc.resolve("sys/kernel/osrelease"), "5.10.160-rockchip-rk3588\n");

        zona(0, "soc-thermal", 54538);
        zona(1, "bigcore0-thermal", 61000);
        zona(2, "gpu-thermal", 50000);
        zona(10, "npu-thermal", 52000);

        interfaceRede("enP4p65s0", true, 5000, 9000);
        interfaceRede("tailscale0", false, 777, 777);
        interfaceRede("lo", false, 123, 123);

        escrever(sys.resolve("devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"), "1800000\n");
        escrever(sys.resolve("devices/system/cpu/cpu1/cpufreq/scaling_cur_freq"), "408000\n");
        escrever(raiz.resolve("hostname"), "orangepi5plus\n");
    }

    private LeitorSistema leitor() {
        return new LeitorSistema(new MonitorProperties(true, 365, proc.toString(), sys.toString(),
                raiz.resolve("hostname").toString(),
                List.of("Raiz=" + raiz, "Inexistente=/caminho/que/nao/existe", "Desmontado=" + raiz.resolve("proc")),
                null, null, raiz.resolve("smart.json").toString(), null, null), java.time.Clock.systemDefaultZone());
    }

    @Test
    void leContadoresDoProc() {
        LeitorSistema.Contadores c = leitor().contadores();

        assertThat(c.cpu()).isEqualTo(new LeitorSistema.CpuTempos(10000, 8500, 500));
        assertThat(c.nucleos()).hasSize(2);
        // só discos inteiros: mmcblk1 + nvme0n1 (partição e zram ficam de fora), em setores de 512 bytes
        assertThat(c.discoLidos()).isEqualTo((100 + 1000) * 512L);
        assertThat(c.discoEscritos()).isEqualTo((200 + 2000) * 512L);
        // só interfaces físicas (com "device")
        assertThat(c.rede()).containsOnlyKeys("enP4p65s0");
        assertThat(c.rede().get("enP4p65s0")).isEqualTo(new LeitorSistema.Trafego(5000, 9000));
    }

    @Test
    void leValoresInstantaneos() {
        LeitorSistema.Instantaneo i = leitor().instantaneo();

        assertThat(i.memTotal()).isEqualTo(16_000_000L * 1024);
        assertThat(i.memUsada()).isEqualTo(4_000_000L * 1024);
        assertThat(i.memPct()).isEqualTo(25.0);
        assertThat(i.swapPct()).isEqualTo(25.0);
        assertThat(i.load1()).isEqualTo(1.11);
        assertThat(i.load15()).isEqualTo(1.83);
        assertThat(i.processos()).isEqualTo(1070);
        assertThat(i.uptimeSegundos()).isEqualTo(156239);
        assertThat(i.frequenciasMhz()).containsExactly(1800L, 408L);

        assertThat(i.temperaturas()).extracting(LeitorSistema.Temperatura::sensor)
                .containsExactly("soc", "bigcore0", "gpu", "npu"); // ordem numérica das zonas (10 depois de 2)
        assertThat(i.temperaturas().get(0).celsius()).isEqualTo(54.5);
        assertThat(i.temperaturaMax()).isEqualTo(61.0);
        assertThat(i.temperatura("soc")).isEqualTo(54.5);
        assertThat(i.temperatura("core", "cpu")).isEqualTo(61.0);
        assertThat(i.temperatura("gpu")).isEqualTo(50.0);
        assertThat(i.temperatura("inexistente")).isNull();

        // caminho que não existe e caminho no mesmo sistema de arquivos de outro (disco desmontado) são ignorados
        assertThat(i.discos()).singleElement().satisfies(d -> {
            assertThat(d.nome()).isEqualTo("Raiz");
            assertThat(d.total()).isPositive();
            assertThat(d.pct()).isBetween(0.0, 100.0);
        });
    }

    @Test
    void identificaOServidor() {
        LeitorSistema.Identificacao id = leitor().identificacao();
        assertThat(id.host()).isEqualTo("orangepi5plus");
        assertThat(id.modelo()).isEqualTo("Orange Pi 5 Plus");
        assertThat(id.kernel()).isEqualTo("5.10.160-rockchip-rk3588");
    }

    @Test
    void usoDeCpuEhADiferencaEntreLeituras() {
        var antes = new LeitorSistema.CpuTempos(10_000, 8_500, 500);
        var depois = new LeitorSistema.CpuTempos(11_000, 9_250, 600); // 1000 jiffies, 750 ociosos, 100 em iowait

        assertThat(depois.usoPct(antes)).isEqualTo(25.0);
        assertThat(depois.iowaitPct(antes)).isEqualTo(10.0);
        assertThat(antes.usoPct(antes)).isZero(); // sem intervalo não divide por zero
    }

    @Test
    void amostraCalculaTaxasPorSegundo() {
        var inst = leitor().instantaneo();
        var antes = new LeitorSistema.Contadores(0, new LeitorSistema.CpuTempos(10_000, 8_500, 500), List.of(),
                Map.of("eth0", new LeitorSistema.Trafego(1_000, 2_000)), 10_000, 20_000);
        var depois = new LeitorSistema.Contadores(60_000_000_000L, new LeitorSistema.CpuTempos(16_000, 13_000, 500), List.of(),
                Map.of("eth0", new LeitorSistema.Trafego(61_000, 122_000), "nova0", new LeitorSistema.Trafego(9_999, 9_999)),
                10_000 + 6_000, 20_000 + 12_000);

        Amostra a = Amostra.de(LocalDateTime.of(2026, 11, 15, 10, 0), antes, depois, inst);

        assertThat(a.intervaloSeg()).isEqualTo(60);
        assertThat(a.cpuPct()).isCloseTo(25.0, within(0.001));
        assertThat(a.redeRxBps()).isCloseTo(1_000, within(0.001)); // interface nova não entra até ter duas leituras
        assertThat(a.redeTxBps()).isCloseTo(2_000, within(0.001));
        assertThat(a.discoLeituraBps()).isCloseTo(100, within(0.001));
        assertThat(a.discoEscritaBps()).isCloseTo(200, within(0.001));
        assertThat(a.memPct()).isEqualTo(25.0);
        assertThat(a.tempMax()).isEqualTo(61.0);
        assertThat(a.tempSoc()).isEqualTo(54.5);
    }

    @Test
    void contadorQueZerouNaoGeraTaxaNegativa() {
        var inst = leitor().instantaneo();
        var antes = new LeitorSistema.Contadores(0, new LeitorSistema.CpuTempos(100, 50, 0), List.of(),
                Map.of("eth0", new LeitorSistema.Trafego(5_000, 5_000)), 5_000, 5_000);
        var depois = new LeitorSistema.Contadores(1_000_000_000L, new LeitorSistema.CpuTempos(200, 100, 0), List.of(),
                Map.of("eth0", new LeitorSistema.Trafego(10, 10)), 10, 10);

        Amostra a = Amostra.de(LocalDateTime.now(), antes, depois, inst);

        assertThat(a.redeRxBps()).isZero();
        assertThat(a.discoEscritaBps()).isZero();
    }

    @Test
    void leituraAoVivoMontaNucleosERede() {
        var leitor = leitor();
        var antes = leitor.contadores();
        var depois = new LeitorSistema.Contadores(antes.nanos() + 500_000_000L,
                new LeitorSistema.CpuTempos(10_100, 8_550, 500),
                List.of(new LeitorSistema.CpuTempos(5_050, 4_250, 250), new LeitorSistema.CpuTempos(5_050, 4_300, 250)),
                Map.of("enP4p65s0", new LeitorSistema.Trafego(5_500, 10_000)), antes.discoLidos(), antes.discoEscritos());

        ServidorAgoraResponse r = ServidorService.montarAgora(LocalDateTime.now(), leitor.identificacao(), antes, depois, leitor.instantaneo(), List.of());

        assertThat(r.host()).isEqualTo("orangepi5plus");
        assertThat(r.cpu().usoPct()).isEqualTo(50.0);
        assertThat(r.cpu().nucleos()).extracting(ServidorAgoraResponse.Nucleo::usoPct).containsExactly(100.0, 0.0);
        assertThat(r.cpu().nucleos()).extracting(ServidorAgoraResponse.Nucleo::mhz).containsExactly(1800L, 408L);
        assertThat(r.rede()).singleElement().satisfies(n -> {
            assertThat(n.interfaceRede()).isEqualTo("enP4p65s0");
            assertThat(n.recebidaBps()).isEqualTo(1_000);
            assertThat(n.enviadaBps()).isEqualTo(2_000);
        });
        assertThat(r.memoria().pct()).isEqualTo(25.0);
        assertThat(r.temperaturaMax()).isEqualTo(61.0);
    }

    @Test
    void decodificaMajorMinorDoDevT() {
        assertThat(LeitorSistema.majorMinor(66305)).isEqualTo("259:1");  // nvme0n1p1
        assertThat(LeitorSistema.majorMinor(2097)).isEqualTo("8:49");    // sdd1
        assertThat(LeitorSistema.majorMinor(45826)).isEqualTo("179:2");  // mmcblk1p2
        // números grandes usam os bits altos (regra do makedev da glibc)
        assertThat(LeitorSistema.majorMinor(makedev(4096, 256))).isEqualTo("4096:256");
        assertThat(LeitorSistema.majorMinor(makedev(300, 70000))).isEqualTo("300:70000");
    }

    private static long makedev(long major, long minor) {
        return ((major & 0xfff) << 8) | ((major & ~0xfffL) << 32) | (minor & 0xff) | ((minor & ~0xffL) << 12);
    }

    private void smart(String lidoEm, String discos) throws IOException {
        escrever(raiz.resolve("smart.json"), "{\"lidoEm\":\"" + lidoEm + "\",\"discos\":[" + discos + "]}");
    }

    @Test
    void temperaturaDoDiscoVemDoSmartPeloDispositivo() throws IOException {
        String dev = LeitorSistema.dispositivo(raiz);
        assertThat(dev).matches("\\d+:\\d+");
        smart(java.time.OffsetDateTime.now().toString(), """
                {"dispositivo":"sdx","modelo":"OUTRO","tipo":"ssd","temperatura":30,"estado":"ativo","dispositivos":["999:0"]},
                {"dispositivo":"sdd","modelo":"WDC WD30PURX","tipo":"hdd","temperatura":56,"estado":"ativo","dispositivos":["0:0","%s"]}
                """.formatted(dev));

        LeitorSistema.EspacoDisco d = leitor().instantaneo().discos().get(0);

        assertThat(d.dispositivo()).isEqualTo(dev);
        assertThat(d.modelo()).isEqualTo("WDC WD30PURX");
        assertThat(d.tipo()).isEqualTo("hdd");
        assertThat(d.temperatura()).isEqualTo(56.0);
        assertThat(d.estadoTemperatura()).isEqualTo("ativo");
    }

    @Test
    void discoEmRepousoFicaSemTemperatura() throws IOException {
        smart(java.time.OffsetDateTime.now().toString(), """
                {"dispositivo":"sdd","tipo":"hdd","temperatura":null,"estado":"repouso","dispositivos":["%s"]}
                """.formatted(LeitorSistema.dispositivo(raiz)));

        LeitorSistema.EspacoDisco d = leitor().instantaneo().discos().get(0);

        assertThat(d.temperatura()).isNull();
        assertThat(d.estadoTemperatura()).isEqualTo("repouso");
    }

    @Test
    void espacoEDispositivoVemDasMontagensDoHost() throws IOException {
        // o caminho é o ponto de montagem no host; nada é medido localmente (o "8:17" não existe aqui)
        escrever(raiz.resolve("smart.json"), """
                {"lidoEm":"%s","discos":[
                  {"dispositivo":"sdb","modelo":"WDC WD30PURX","tipo":"hdd","temperatura":57,"estado":"ativo",
                   "dispositivos":["8:16","8:17"]}],
                 "montagens":[
                  {"ponto":"/","dispositivo":"179:2","total":1000,"usado":100,"disponivel":850},
                  {"ponto":"%s","dispositivo":"8:17","total":4000,"usado":1000,"disponivel":3000},
                  {"ponto":"/sem/espaco","dispositivo":"8:1"}]}
                """.formatted(java.time.OffsetDateTime.now(), raiz + "/"));

        // "Inexistente" e "Desmontado" não estão nas montagens do host: ficam de fora
        assertThat(leitor().instantaneo().discos()).singleElement().satisfies(d -> {
            assertThat(d.nome()).isEqualTo("Raiz");
            assertThat(d.total()).isEqualTo(4000);
            assertThat(d.usado()).isEqualTo(1000);
            assertThat(d.pct()).isEqualTo(25.0);
            assertThat(d.dispositivo()).isEqualTo("8:17");
            assertThat(d.modelo()).isEqualTo("WDC WD30PURX");
            assertThat(d.temperatura()).isEqualTo(57.0);
        });
        assertThat(LeitorSistema.lerArquivoSmart(raiz.resolve("smart.json"), java.time.OffsetDateTime.now())
                .montagens()).hasSize(2); // a sem espaço é ignorada
    }

    @Test
    void smartVelhoInvalidoOuAusenteEhIgnorado() throws IOException {
        Path arquivo = raiz.resolve("smart.json");
        var agora = java.time.OffsetDateTime.parse("2026-11-15T12:00:00-03:00");
        String disco = "{\"dispositivo\":\"sda\",\"temperatura\":40,\"dispositivos\":[\"8:1\"]}";

        smart("2026-11-15T11:56:00-03:00", disco);
        assertThat(LeitorSistema.lerSmart(arquivo, agora)).singleElement()
                .satisfies(s -> assertThat(s.temperatura()).isEqualTo(40.0));

        smart("2026-11-15T11:50:00-03:00", disco); // 10 minutos: o timer do host parou
        assertThat(LeitorSistema.lerSmart(arquivo, agora)).isEmpty();

        escrever(arquivo, "{ isto não é json");
        assertThat(LeitorSistema.lerSmart(arquivo, agora)).isEmpty();
        assertThat(LeitorSistema.lerSmart(raiz.resolve("nao-existe.json"), agora)).isEmpty();
        assertThat(LeitorSistema.lerSmart(null, agora)).isEmpty();
    }

    @Test
    void leASaudeDoSmart() throws IOException {
        Path arquivo = raiz.resolve("smart.json");
        var agora = java.time.OffsetDateTime.parse("2026-11-15T12:00:00-03:00");
        smart("2026-11-15T11:59:40-03:00", """
                {"dispositivo":"sdc","temperatura":54,"dispositivos":["8:32"],"saude":{
                  "lidoEm":"2026-11-15T11:59:40-03:00","aprovado":true,"horasLigado":7881,"vezesLigado":2395,
                  "temperaturaMinVida":0,"temperaturaMaxVida":61,"temperaturaLimite":55,"temperaturaLimiteAbsoluto":60,
                  "vezesAcimaLimite":106,"atributos":{"5":0,"187":56928,"199":18,"x":1},
                  "falhas":[{"atributo":"Airflow_Temperature_Cel","quando":"past"}],"nvme":null}}
                """);

        SaudeSmart s = LeitorSistema.lerSmart(arquivo, agora).get(0).saude();

        assertThat(s.aprovado()).isTrue();
        assertThat(s.horasLigado()).isEqualTo(7881L);
        assertThat(s.temperaturaMinVidaValida()).isNull(); // 0 = o disco não sabe
        assertThat(s.temperaturaLimite()).isEqualTo(55.0);
        assertThat(s.atributo(SaudeSmart.ERROS_NAO_CORRIGIDOS)).isEqualTo(56_928L);
        assertThat(s.atributos()).hasSize(3);             // "x" não é número de atributo
        assertThat(s.falhas()).singleElement().satisfies(f -> assertThat(f.agora()).isFalse());
        assertThat(s.nvme()).isNull();
    }

    @Test
    void smartSemSaudeContinuaFuncionando() throws IOException {
        Path arquivo = raiz.resolve("smart.json");
        var agora = java.time.OffsetDateTime.parse("2026-11-15T12:00:00-03:00");
        smart("2026-11-15T11:59:40-03:00", "{\"dispositivo\":\"sda\",\"temperatura\":40,\"dispositivos\":[\"8:1\"]}");
        assertThat(LeitorSistema.lerSmart(arquivo, agora).get(0).saude()).isNull();
    }

    @Test
    void nomeDoSensorSemSufixoThermal() {
        assertThat(LeitorSistema.nomeSensor("soc-thermal")).isEqualTo("soc");
        assertThat(LeitorSistema.nomeSensor("bigcore0_thermal")).isEqualTo("bigcore0");
        assertThat(LeitorSistema.nomeSensor("cpu")).isEqualTo("cpu");
    }

    @Test
    void discosConfiguradosComNome() {
        var props = new MonitorProperties(null, null, null, null, null,
                List.of("NVMe=/", " Mídia = /host/discos/media ", "/sem/nome"), " http://proxy:2375/ ", null, " ", null, null);
        assertThat(props.discosMonitorados()).containsExactly(
                new MonitorProperties.DiscoMonitorado("NVMe", "/"),
                new MonitorProperties.DiscoMonitorado("Mídia", "/host/discos/media"),
                new MonitorProperties.DiscoMonitorado("/sem/nome", "/sem/nome"));
        assertThat(props.retencaoDias()).isEqualTo(365);
        assertThat(props.habilitado()).isTrue();
        assertThat(props.dockerUrl()).isEqualTo("http://proxy:2375");
        assertThat(props.retencaoContainersDias()).isEqualTo(90);
        assertThat(props.smartArquivo()).isNull();
        assertThat(new MonitorProperties(null, null, null, null, null, null, " ", null, null, null, null).containersHabilitados()).isFalse();
    }

    private void zona(int n, String tipo, int miliGraus) throws IOException {
        Path z = sys.resolve("class/thermal/thermal_zone" + n);
        escrever(z.resolve("type"), tipo + "\n");
        escrever(z.resolve("temp"), miliGraus + "\n");
    }

    private void interfaceRede(String nome, boolean fisica, long rx, long tx) throws IOException {
        Path i = sys.resolve("class/net/" + nome);
        escrever(i.resolve("statistics/rx_bytes"), rx + "\n");
        escrever(i.resolve("statistics/tx_bytes"), tx + "\n");
        if (fisica) Files.createDirectories(i.resolve("device"));
    }

    private static void escrever(Path p, String conteudo) throws IOException {
        Files.createDirectories(p.getParent());
        Files.writeString(p, conteudo);
    }
}
