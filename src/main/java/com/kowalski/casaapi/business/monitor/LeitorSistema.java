package com.kowalski.casaapi.business.monitor;

import com.kowalski.casaapi.config.monitor.MonitorProperties;
import com.kowalski.casaapi.config.monitor.MonitorProperties.DiscoMonitorado;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
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
    private static final Pattern POLITICA = Pattern.compile("policy(\\d+)");
    private static final Pattern MEDIA10 = Pattern.compile("avg10=([0-9.]+)");
    private static final long SETOR = 512;

    private final Path proc;
    private final Path sys;
    private final Path arquivoHostname;
    private final List<DiscoMonitorado> discos;
    private final Path arquivoSmart;

    public LeitorSistema(MonitorProperties props) {
        this.proc = Path.of(props.procPath());
        this.sys = Path.of(props.sysPath());
        this.arquivoHostname = Path.of(props.hostnameArquivo());
        this.discos = props.discosMonitorados();
        this.arquivoSmart = props.smartArquivo() == null ? null : Path.of(props.smartArquivo());
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

    /**
     * Contadores de um disco inteiro no /proc/diskstats.
     *
     * @param operacoes leituras + escritas concluídas
     * @param msEspera  tempo somado das operações (ms), fila incluída
     * @param msOcupado tempo com alguma operação em andamento (ms)
     */
    public record DiscoContadores(long operacoes, long msEspera, long msOcupado) {
    }

    /** Ocupação e latência de um disco no intervalo. */
    public record DiscoAtividade(String disco, double ocupacaoPct, Double latenciaMs) {
    }

    /** Contadores acumulados; a diferença entre duas leituras dá as taxas do intervalo. */
    public record Contadores(long nanos, CpuTempos cpu, List<CpuTempos> nucleos, Map<String, Trafego> rede,
                             long discoLidos, long discoEscritos, Map<String, DiscoContadores> discosIo,
                             long redeErros) {

        public Contadores(long nanos, CpuTempos cpu, List<CpuTempos> nucleos, Map<String, Trafego> rede,
                          long discoLidos, long discoEscritos) {
            this(nanos, cpu, nucleos, rede, discoLidos, discoEscritos, Map.of(), 0);
        }

        public double segundosDesde(Contadores antes) {
            return Math.max((nanos - antes.nanos) / 1e9, 0.001);
        }

        /** Ocupação e latência média por operação de cada disco que existia nas duas leituras. */
        public List<DiscoAtividade> atividadeDiscos(Contadores antes) {
            double ms = segundosDesde(antes) * 1000;
            List<DiscoAtividade> lista = new ArrayList<>();
            discosIo.forEach((nome, d) -> {
                DiscoContadores a = antes.discosIo.get(nome);
                if (a == null) return;
                long ops = d.operacoes() - a.operacoes();
                long espera = d.msEspera() - a.msEspera();
                long ocupado = Math.max(d.msOcupado() - a.msOcupado(), 0);
                lista.add(new DiscoAtividade(nome, limitar(100.0 * ocupado / ms),
                        ops > 0 && espera >= 0 ? (double) espera / ops : null));
            });
            return lista;
        }

        /** Latência média por operação somando todos os discos; nula sem operações no intervalo. */
        public Double latenciaDiscosMs(Contadores antes) {
            long ops = 0;
            long espera = 0;
            for (var e : discosIo.entrySet()) {
                DiscoContadores a = antes.discosIo.get(e.getKey());
                if (a == null) continue;
                long o = e.getValue().operacoes() - a.operacoes();
                long t = e.getValue().msEspera() - a.msEspera();
                if (o <= 0 || t < 0) continue;
                ops += o;
                espera += t;
            }
            return ops > 0 ? (double) espera / ops : null;
        }

        /** Ocupação do disco mais ocupado (%); 100% = sempre com operação em andamento. */
        public Double ocupacaoMaxDiscosPct(Contadores antes) {
            return atividadeDiscos(antes).stream().map(DiscoAtividade::ocupacaoPct).max(Double::compare).orElse(null);
        }

        /** Erros e pacotes descartados nas interfaces físicas no intervalo (zero se o contador zerou). */
        public long redeErrosDesde(Contadores antes) {
            return Math.max(redeErros - antes.redeErros, 0);
        }
    }

    /**
     * Pressão (PSI) nos últimos 10 s: % do tempo em que alguma tarefa (some) ou todas (full) esperaram pelo recurso.
     * Nulos se o kernel não tem PSI.
     */
    public record Pressao(Double cpu, Double memoria, Double memoriaTotal, Double io, Double ioTotal) {
    }

    /** Um grupo de núcleos que muda de frequência junto (policy do cpufreq). */
    public record Cluster(String nome, List<Integer> nucleos, Long atualMhz, Long maxMhz, Long limiteMhz) {

        /** Limite atual em % do máximo do hardware; abaixo de 100 o kernel está segurando a frequência (calor). */
        public Double limitePct() {
            return maxMhz == null || limiteMhz == null || maxMhz <= 0 ? null : limitar(100.0 * limiteMhz / maxMhz);
        }
    }

    public record Temperatura(String sensor, double celsius) {
    }

    /**
     * Espaço de um disco e, quando o arquivo do SMART tem o disco, a temperatura.
     *
     * @param dispositivo       major:minor do sistema de arquivos do caminho (ex.: "8:49")
     * @param tipo              nvme, ssd ou hdd (do SMART); nulo sem SMART
     * @param estadoTemperatura ativo, repouso (disco parado, não foi acordado para ler) ou sem_dados
     */
    public record EspacoDisco(String nome, String caminho, long total, long usado, double pct, String dispositivo,
                              String modelo, String tipo, Double temperatura, String estadoTemperatura,
                              SaudeSmart saude) {

        public EspacoDisco(String nome, String caminho, long total, long usado, double pct) {
            this(nome, caminho, total, usado, pct, null, null, null, null, null, null);
        }
    }

    /** Um disco no arquivo do SMART. */
    public record DiscoSmart(String dispositivo, String modelo, String tipo, Double temperatura, String estado,
                             List<String> dispositivos, SaudeSmart saude) {
    }

    /** Leituras do SMART mais velhas que isso são ignoradas (o timer do host parou). */
    static final Duration SMART_VALIDADE = Duration.ofMinutes(5);

    /** Valores que já são instantâneos (não precisam de duas leituras). */
    public record Instantaneo(long memTotal, long memDisponivel, long swapTotal, long swapLivre,
                              double load1, double load5, double load15, int processos, long uptimeSegundos,
                              List<Temperatura> temperaturas, List<EspacoDisco> discos, List<Long> frequenciasMhz,
                              Pressao pressao, Double gpuPct, List<Cluster> clusters) {

        public Instantaneo(long memTotal, long memDisponivel, long swapTotal, long swapLivre,
                           double load1, double load5, double load15, int processos, long uptimeSegundos,
                           List<Temperatura> temperaturas, List<EspacoDisco> discos, List<Long> frequenciasMhz) {
            this(memTotal, memDisponivel, swapTotal, swapLivre, load1, load5, load15, processos, uptimeSegundos,
                    temperaturas, discos, frequenciasMhz, new Pressao(null, null, null, null, null), null, List.of());
        }

        /** Menor limite entre os clusters (% do máximo); 100 = nenhum núcleo limitado. */
        public Double cpuLimitePct() {
            return clusters.stream().map(Cluster::limitePct).filter(java.util.Objects::nonNull)
                    .min(Double::compare).orElse(null);
        }

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
        List<String> diskstats = linhas(proc.resolve("diskstats"));
        long[] disco = parseDiskstats(diskstats);
        return new Contadores(System.nanoTime(), cpus.get(0), cpus.subList(1, cpus.size()), lerRede(),
                disco[0], disco[1], parseDiskstatsPorDisco(diskstats), lerErrosRede());
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
                lerTemperaturas(), lerDiscos(), lerFrequencias(), lerPressao(), lerGpu(), lerClusters());
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

    /** Contadores de operações e tempo de cada disco inteiro (sem partições, zram e loop). */
    static Map<String, DiscoContadores> parseDiskstatsPorDisco(List<String> linhas) {
        Map<String, DiscoContadores> discos = new LinkedHashMap<>();
        for (String linha : linhas) {
            String[] p = linha.trim().split("\\s+");
            if (p.length < 13 || !DISCO_INTEIRO.matcher(p[2]).matches()) continue;
            // 3 leituras, 6 ms lendo, 7 escritas, 10 ms escrevendo, 12 ms com I/O em andamento
            discos.put(p[2], new DiscoContadores(Long.parseLong(p[3]) + Long.parseLong(p[7]),
                    Long.parseLong(p[6]) + Long.parseLong(p[10]), Long.parseLong(p[12])));
        }
        return discos;
    }

    /** avg10 da linha "some" ou "full" de um arquivo do /proc/pressure; nulo se não houver. */
    static Double parsePressao(List<String> linhas, String tipo) {
        for (String linha : linhas) {
            if (!linha.startsWith(tipo + " ")) continue;
            Matcher m = MEDIA10.matcher(linha);
            if (m.find()) return Double.parseDouble(m.group(1));
        }
        return null;
    }

    /** "37@800000000Hz" (load do devfreq) vira 37. */
    static Double parseCargaDevfreq(String texto) {
        if (texto == null) return null;
        int i = texto.indexOf('@');
        try {
            return limitar(Double.parseDouble((i < 0 ? texto : texto.substring(0, i)).trim()));
        } catch (NumberFormatException e) {
            return null;
        }
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

    /** Soma de erros e descartes (rx/tx) das interfaces físicas. */
    private long lerErrosRede() {
        long total = 0;
        Path dir = sys.resolve("class/net");
        if (!Files.isDirectory(dir)) return 0;
        for (Path iface : ordenados(dir)) {
            if (!Files.exists(iface.resolve("device"))) continue;
            for (String c : List.of("rx_errors", "tx_errors", "rx_dropped", "tx_dropped")) {
                Long v = numero(iface.resolve("statistics/" + c));
                if (v != null) total += v;
            }
        }
        return total;
    }

    private Pressao lerPressao() {
        Path dir = proc.resolve("pressure");
        List<String> cpu = linhasOuVazio(dir.resolve("cpu"));
        List<String> mem = linhasOuVazio(dir.resolve("memory"));
        List<String> io = linhasOuVazio(dir.resolve("io"));
        return new Pressao(parsePressao(cpu, "some"), parsePressao(mem, "some"), parsePressao(mem, "full"),
                parsePressao(io, "some"), parsePressao(io, "full"));
    }

    /** Carga da GPU pelo devfreq (Mali do RK3588: {@code /sys/class/devfreq/*.gpu/load}). */
    private Double lerGpu() {
        Path dir = sys.resolve("class/devfreq");
        if (!Files.isDirectory(dir)) return null;
        for (Path d : ordenados(dir)) {
            if (d.getFileName().toString().toLowerCase().contains("gpu")) return parseCargaDevfreq(textoOuNulo(d.resolve("load")));
        }
        return null;
    }

    /** Frequência atual, máxima do hardware e limite atual de cada policy do cpufreq. */
    private List<Cluster> lerClusters() {
        List<Cluster> lista = new ArrayList<>();
        Path dir = sys.resolve("devices/system/cpu/cpufreq");
        if (!Files.isDirectory(dir)) return lista;
        List<Path> politicas = new ArrayList<>(ordenados(dir).stream()
                .filter(p -> POLITICA.matcher(p.getFileName().toString()).matches()).toList());
        politicas.sort(Comparator.comparingInt(p -> numeroNoNome(POLITICA, p)));
        for (Path p : politicas) {
            List<Integer> nucleos = new ArrayList<>();
            String rel = textoOuNulo(p.resolve("related_cpus"));
            if (rel != null) {
                for (String n : rel.split("\\s+")) {
                    try {
                        nucleos.add(Integer.parseInt(n));
                    } catch (NumberFormatException e) {
                        // ignora
                    }
                }
            }
            lista.add(new Cluster(p.getFileName().toString(), List.copyOf(nucleos), mhz(numero(p.resolve("scaling_cur_freq"))),
                    mhz(numero(p.resolve("cpuinfo_max_freq"))), mhz(numero(p.resolve("scaling_max_freq")))));
        }
        return lista;
    }

    private static Long mhz(Long khz) {
        return khz == null ? null : khz / 1000;
    }

    /** Momento do boot (linha btime do /proc/stat, segundos desde a época); nulo se não der para ler. */
    public Long bootEpoch() {
        for (String linha : linhasOuVazio(proc.resolve("stat"))) {
            if (linha.startsWith("btime ")) {
                try {
                    return Long.parseLong(linha.substring(6).trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static List<String> linhasOuVazio(Path p) {
        try {
            return Files.readAllLines(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return List.of();
        }
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
        List<DiscoSmart> smart = lerSmart(arquivoSmart, OffsetDateTime.now());
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
            String dispositivo = dispositivo(f.toPath());
            DiscoSmart s = dispositivo == null ? null : smart.stream()
                    .filter(x -> x.dispositivos().contains(dispositivo)).findFirst().orElse(null);
            lista.add(s == null
                    ? new EspacoDisco(d.nome(), d.caminho(), total, usado, pct, dispositivo, null, null, null, null, null)
                    : new EspacoDisco(d.nome(), d.caminho(), total, usado, pct, dispositivo, s.modelo(), s.tipo(),
                            s.temperatura(), s.estado(), s.saude()));
        }
        return lista;
    }

    /** major:minor do sistema de arquivos onde o caminho está (o st_dev do stat), ou nulo se não der para saber. */
    static String dispositivo(Path caminho) {
        try {
            Object dev = Files.getAttribute(caminho, "unix:dev");
            return dev instanceof Number n ? majorMinor(n.longValue()) : null;
        } catch (IOException | UnsupportedOperationException | IllegalArgumentException e) {
            return null;
        }
    }

    /** Decodifica um dev_t do Linux (mesma regra das macros major()/minor() da glibc). */
    static String majorMinor(long dev) {
        long major = ((dev >>> 8) & 0xfffL) | ((dev >>> 32) & 0xfffff000L);
        long minor = (dev & 0xffL) | ((dev >>> 12) & 0xffffff00L);
        return major + ":" + minor;
    }

    /** Discos do arquivo do SMART; lista vazia se o arquivo não existe, está inválido ou velho demais. */
    static List<DiscoSmart> lerSmart(Path arquivo, OffsetDateTime agora) {
        if (arquivo == null || !Files.isReadable(arquivo)) return List.of();
        try {
            Map<?, ?> json = JsonMapper.shared().readValue(arquivo, Map.class);
            OffsetDateTime lidoEm = OffsetDateTime.parse(String.valueOf(json.get("lidoEm")));
            if (lidoEm.isBefore(agora.minus(SMART_VALIDADE))) return List.of();
            List<DiscoSmart> lista = new ArrayList<>();
            if (json.get("discos") instanceof List<?> discos) {
                for (Object o : discos) {
                    if (!(o instanceof Map<?, ?> d)) continue;
                    List<String> devs = d.get("dispositivos") instanceof List<?> l
                            ? l.stream().map(String::valueOf).toList() : List.of();
                    lista.add(new DiscoSmart(textoDe(d.get("dispositivo")), textoDe(d.get("modelo")),
                            textoDe(d.get("tipo")), decimal(d.get("temperatura")), textoDe(d.get("estado")), devs,
                            d.get("saude") instanceof Map<?, ?> saude ? lerSaude(saude) : null));
                }
            }
            return lista;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /** O bloco "saude" de um disco; campos ausentes ou inválidos ficam nulos. */
    static SaudeSmart lerSaude(Map<?, ?> s) {
        OffsetDateTime lidoEm;
        try {
            lidoEm = s.get("lidoEm") == null ? null : OffsetDateTime.parse(String.valueOf(s.get("lidoEm")));
        } catch (RuntimeException e) {
            lidoEm = null;
        }
        Map<Integer, Long> atributos = new LinkedHashMap<>();
        if (s.get("atributos") instanceof Map<?, ?> m) {
            m.forEach((id, valor) -> {
                try {
                    Long v = inteiro(valor);
                    if (v != null) atributos.put(Integer.parseInt(String.valueOf(id)), v);
                } catch (NumberFormatException e) {
                    // id que não é número: ignora
                }
            });
        }
        List<SaudeSmart.Falha> falhas = new ArrayList<>();
        if (s.get("falhas") instanceof List<?> l) {
            for (Object o : l) {
                if (o instanceof Map<?, ?> f) falhas.add(new SaudeSmart.Falha(textoDe(f.get("atributo")), textoDe(f.get("quando"))));
            }
        }
        SaudeSmart.Nvme nvme = s.get("nvme") instanceof Map<?, ?> n ? new SaudeSmart.Nvme(inteiro(n.get("avisoCritico")),
                inteiro(n.get("reservaDisponivel")), inteiro(n.get("reservaMinima")), inteiro(n.get("desgaste")),
                inteiro(n.get("errosMidia")), inteiro(n.get("errosRegistrados")), inteiro(n.get("desligamentosInesperados")),
                inteiro(n.get("minutosAcimaAviso")), inteiro(n.get("minutosAcimaCritico")), inteiro(n.get("bytesEscritos")),
                inteiro(n.get("bytesLidos"))) : null;
        return new SaudeSmart(lidoEm, s.get("aprovado") instanceof Boolean b ? b : null, inteiro(s.get("horasLigado")),
                inteiro(s.get("vezesLigado")), decimal(s.get("temperaturaMinVida")), decimal(s.get("temperaturaMaxVida")),
                decimal(s.get("temperaturaLimite")), decimal(s.get("temperaturaLimiteAbsoluto")),
                inteiro(s.get("vezesAcimaLimite")), Map.copyOf(atributos), List.copyOf(falhas), nvme);
    }

    private static Double decimal(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    private static Long inteiro(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }

    private static String textoDe(Object o) {
        return o == null ? null : o.toString();
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
