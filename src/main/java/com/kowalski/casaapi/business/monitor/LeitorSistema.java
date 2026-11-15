package com.kowalski.casaapi.business.monitor;

import com.kowalski.casaapi.config.monitor.MonitorProperties;
import com.kowalski.casaapi.config.monitor.MonitorProperties.DiscoMonitorado;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê as métricas do servidor direto do kernel, sem depender de outro serviço:
 * <ul>
 *   <li>{@code /proc/stat}, {@code /proc/meminfo}, {@code /proc/loadavg}, {@code /proc/uptime} e
 *       {@code /proc/diskstats}: CPU, memória, carga e discos (no container já mostram o host inteiro);</li>
 *   <li>{@code /sys/class/thermal}: temperaturas;</li>
 *   <li>{@code /sys/class/net}: tráfego das interfaces físicas (precisa do /sys do host montado);</li>
 *   <li>espaço em disco dos caminhos configurados em {@code monitor.discos}.</li>
 * </ul>
 * Contadores acumulados (CPU, rede, disco) viram taxa comparando duas leituras: veja {@link Contadores}.
 */
@Component
public class LeitorSistema {

    private static final Pattern DISCO_INTEIRO = Pattern.compile("nvme\\d+n\\d+|mmcblk\\d+|sd[a-z]+|vd[a-z]+|hd[a-z]+");
    private static final Pattern ZONA_TERMICA = Pattern.compile("thermal_zone(\\d+)");
    private static final Pattern NUCLEO = Pattern.compile("cpu(\\d+)");
    private static final long SETOR = 512;

    private final Path proc;
    private final Path sys;
    private final Path arquivoHostname;
    private final List<DiscoMonitorado> discos;

    public LeitorSistema(MonitorProperties props) {
        this.proc = Path.of(props.procPath());
        this.sys = Path.of(props.sysPath());
        this.arquivoHostname = Path.of(props.hostnameArquivo());
        this.discos = props.discosMonitorados();
    }

    /** Tempos de CPU em jiffies desde o boot. */
    public record CpuTempos(long total, long ocioso, long iowait) {

        /** Percentual de uso entre {@code antes} e esta leitura. */
        public double usoPct(CpuTempos antes) {
            long dt = total - antes.total;
            return dt <= 0 ? 0 : limitar(100.0 * (dt - (ocioso - antes.ocioso)) / dt);
        }

        public double iowaitPct(CpuTempos antes) {
            long dt = total - antes.total;
            return dt <= 0 ? 0 : limitar(100.0 * (iowait - antes.iowait) / dt);
        }
    }

    public record Trafego(long recebidos, long enviados) {
    }

    /** Contadores acumulados; a diferença entre duas leituras dá as taxas do intervalo. */
    public record Contadores(long nanos, CpuTempos cpu, List<CpuTempos> nucleos, Map<String, Trafego> rede,
                             long discoLidos, long discoEscritos) {

        public double segundosDesde(Contadores antes) {
            return Math.max((nanos - antes.nanos) / 1e9, 0.001);
        }
    }

    public record Temperatura(String sensor, double celsius) {
    }

    public record EspacoDisco(String nome, String caminho, long total, long usado, double pct) {
    }

    /** Valores que já são instantâneos (não precisam de duas leituras). */
    public record Instantaneo(long memTotal, long memDisponivel, long swapTotal, long swapLivre,
                              double load1, double load5, double load15, int processos, long uptimeSegundos,
                              List<Temperatura> temperaturas, List<EspacoDisco> discos, List<Long> frequenciasMhz) {

        public long memUsada() {
            return Math.max(memTotal - memDisponivel, 0);
        }

        public long swapUsada() {
            return Math.max(swapTotal - swapLivre, 0);
        }

        public double memPct() {
            return memTotal > 0 ? limitar(100.0 * memUsada() / memTotal) : 0;
        }

        public double swapPct() {
            return swapTotal > 0 ? limitar(100.0 * swapUsada() / swapTotal) : 0;
        }

        public Double temperaturaMax() {
            return temperaturas.stream().map(Temperatura::celsius).max(Double::compare).orElse(null);
        }

        /** Maior temperatura entre os sensores cujo nome contém algum dos trechos. */
        public Double temperatura(String... trechos) {
            return temperaturas.stream()
                    .filter(t -> {
                        String s = t.sensor().toLowerCase();
                        for (String trecho : trechos) if (s.contains(trecho)) return true;
                        return false;
                    })
                    .map(Temperatura::celsius).max(Double::compare).orElse(null);
        }
    }

    public record Identificacao(String host, String modelo, String kernel) {
    }

    public Contadores contadores() {
        List<CpuTempos> cpus = parseStat(linhas(proc.resolve("stat")));
        long[] disco = parseDiskstats(linhas(proc.resolve("diskstats")));
        return new Contadores(System.nanoTime(), cpus.get(0), cpus.subList(1, cpus.size()), lerRede(),
                disco[0], disco[1]);
    }

    public Instantaneo instantaneo() {
        Map<String, Long> mem = parseMeminfo(linhas(proc.resolve("meminfo")));
        String[] load = texto(proc.resolve("loadavg")).split("\\s+");
        String[] uptime = texto(proc.resolve("uptime")).split("\\s+");
        int processos = load.length > 3 && load[3].contains("/") ? Integer.parseInt(load[3].split("/")[1]) : 0;
        return new Instantaneo(
                mem.getOrDefault("MemTotal", 0L), mem.getOrDefault("MemAvailable", 0L),
                mem.getOrDefault("SwapTotal", 0L), mem.getOrDefault("SwapFree", 0L),
                Double.parseDouble(load[0]), Double.parseDouble(load[1]), Double.parseDouble(load[2]), processos,
                (long) Double.parseDouble(uptime[0]),
                lerTemperaturas(), lerDiscos(), lerFrequencias());
    }

    public Identificacao identificacao() {
        String host = textoOuNulo(arquivoHostname);
        if (host == null) {
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (IOException e) {
                host = "servidor";
            }
        }
        String modelo = textoOuNulo(proc.resolve("device-tree/model"));
        if (modelo == null) modelo = textoOuNulo(sys.resolve("firmware/devicetree/base/model"));
        if (modelo == null) modelo = textoOuNulo(sys.resolve("devices/virtual/dmi/id/product_name"));
        return new Identificacao(host, modelo, textoOuNulo(proc.resolve("sys/kernel/osrelease")));
    }

    // ---- parsers (estáticos para os testes) ----

    /** Primeira posição: CPU total; depois, um item por núcleo, na ordem do arquivo. */
    static List<CpuTempos> parseStat(List<String> linhas) {
        List<CpuTempos> cpus = new ArrayList<>();
        for (String linha : linhas) {
            if (!linha.startsWith("cpu")) continue;
            String[] p = linha.trim().split("\\s+");
            long total = 0;
            // user nice system idle iowait irq softirq steal (guest já está contado em user)
            for (int i = 1; i <= Math.min(8, p.length - 1); i++) total += Long.parseLong(p[i]);
            long idle = Long.parseLong(p[4]);
            long iowait = p.length > 5 ? Long.parseLong(p[5]) : 0;
            cpus.add(new CpuTempos(total, idle + iowait, iowait));
        }
        if (cpus.isEmpty()) throw new IllegalStateException("/proc/stat sem linhas de CPU");
        return cpus;
    }

    /** Valores em bytes (o arquivo vem em kB). */
    static Map<String, Long> parseMeminfo(List<String> linhas) {
        Map<String, Long> mem = new LinkedHashMap<>();
        for (String linha : linhas) {
            int i = linha.indexOf(':');
            if (i < 0) continue;
            String[] valor = linha.substring(i + 1).trim().split("\\s+");
            long n = Long.parseLong(valor[0]);
            mem.put(linha.substring(0, i), valor.length > 1 && valor[1].equalsIgnoreCase("kB") ? n * 1024 : n);
        }
        return mem;
    }

    /** Bytes lidos e escritos, somando só os discos inteiros (sem partições, zram e loop). */
    static long[] parseDiskstats(List<String> linhas) {
        long lidos = 0;
        long escritos = 0;
        for (String linha : linhas) {
            String[] p = linha.trim().split("\\s+");
            if (p.length < 10 || !DISCO_INTEIRO.matcher(p[2]).matches()) continue;
            lidos += Long.parseLong(p[5]) * SETOR;
            escritos += Long.parseLong(p[9]) * SETOR;
        }
        return new long[]{lidos, escritos};
    }

    /** "soc-thermal" vira "soc"; "bigcore0_thermal" vira "bigcore0". */
    static String nomeSensor(String tipo) {
        return tipo.trim().replaceAll("[-_ ]?thermal$", "");
    }

    static double limitar(double pct) {
        return Math.max(0, Math.min(100, pct));
    }

    // ---- leitura ----

    /** Interfaces físicas (com {@code device}): ignora lo, bridges, veth e VPN, que repetiriam o mesmo tráfego. */
    private Map<String, Trafego> lerRede() {
        Map<String, Trafego> rede = new LinkedHashMap<>();
        Path dir = sys.resolve("class/net");
        if (!Files.isDirectory(dir)) return rede;
        for (Path iface : ordenados(dir)) {
            if (!Files.exists(iface.resolve("device"))) continue;
            Long rx = numero(iface.resolve("statistics/rx_bytes"));
            Long tx = numero(iface.resolve("statistics/tx_bytes"));
            if (rx != null && tx != null) rede.put(iface.getFileName().toString(), new Trafego(rx, tx));
        }
        return rede;
    }

    private List<Temperatura> lerTemperaturas() {
        List<Temperatura> temps = new ArrayList<>();
        Path dir = sys.resolve("class/thermal");
        if (!Files.isDirectory(dir)) return temps;
        List<Path> zonas = new ArrayList<>(ordenados(dir).stream()
                .filter(p -> ZONA_TERMICA.matcher(p.getFileName().toString()).matches()).toList());
        zonas.sort(Comparator.comparingInt(p -> numeroNoNome(ZONA_TERMICA, p)));
        for (Path zona : zonas) {
            String tipo = textoOuNulo(zona.resolve("type"));
            Long mili = numero(zona.resolve("temp"));
            if (tipo == null || mili == null || mili <= 0) continue;
            temps.add(new Temperatura(nomeSensor(tipo), Math.round(mili / 100.0) / 10.0));
        }
        return temps;
    }

    /**
     * Espaço dos discos configurados. Caminhos que caem num sistema de arquivos já medido são ignorados: assim um
     * disco externo desmontado (cuja pasta passa a ficar no disco do sistema) não aparece com os números do sistema.
     */
    private List<EspacoDisco> lerDiscos() {
        List<EspacoDisco> lista = new ArrayList<>();
        Set<String> medidos = new HashSet<>();
        for (DiscoMonitorado d : discos) {
            File f = new File(d.caminho());
            long total = f.getTotalSpace();
            if (total <= 0) continue;
            long livre = f.getFreeSpace();
            long disponivel = f.getUsableSpace();
            if (!medidos.add(total + ":" + livre + ":" + disponivel)) continue;
            long usado = total - livre;
            // mesmo cálculo do df: o espaço reservado ao root não conta como disponível
            double pct = usado + disponivel > 0 ? limitar(100.0 * usado / (usado + disponivel)) : 0;
            lista.add(new EspacoDisco(d.nome(), d.caminho(), total, usado, pct));
        }
        return lista;
    }

    private List<Long> lerFrequencias() {
        List<Long> mhz = new ArrayList<>();
        Path dir = sys.resolve("devices/system/cpu");
        if (!Files.isDirectory(dir)) return mhz;
        List<Path> nucleos = new ArrayList<>(ordenados(dir).stream()
                .filter(p -> NUCLEO.matcher(p.getFileName().toString()).matches()).toList());
        nucleos.sort(Comparator.comparingInt(p -> numeroNoNome(NUCLEO, p)));
        for (Path n : nucleos) {
            Long khz = numero(n.resolve("cpufreq/scaling_cur_freq"));
            mhz.add(khz == null ? null : khz / 1000);
        }
        return mhz;
    }

    private static int numeroNoNome(Pattern padrao, Path p) {
        Matcher m = padrao.matcher(p.getFileName().toString());
        return m.matches() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
    }

    private static List<Path> ordenados(Path dir) {
        List<Path> itens = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            ds.forEach(itens::add);
        } catch (IOException e) {
            return itens;
        }
        itens.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return itens;
    }

    private static List<String> linhas(Path p) {
        try {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + p, e);
        }
    }

    private static String texto(Path p) {
        return String.join("\n", linhas(p)).trim();
    }

    private static String textoOuNulo(Path p) {
        try {
            String s = new String(Files.readAllBytes(p), StandardCharsets.UTF_8).replace("\0", "").trim();
            return s.isEmpty() ? null : s;
        } catch (IOException e) {
            return null;
        }
    }

    private static Long numero(Path p) {
        String s = textoOuNulo(p);
        if (s == null) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
