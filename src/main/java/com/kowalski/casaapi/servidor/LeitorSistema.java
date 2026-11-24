package com.kowalski.casaapi.servidor;

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
import java.time.Clock;
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
    private final Clock relogio;

    public LeitorSistema(MonitorProperties props, Clock relogio) {
        this.relogio = relogio;
        this.proc = Path.of(props.procPath());
        this.sys = Path.of(props.sysPath());
        this.arquivoHostname = Path.of(props.hostnameArquivo());
        this.discos = props.discosMonitorados();
        this.arquivoSmart = props.smartArquivo() == null ? null : Path.of(props.smartArquivo());
    }

    public record CpuTempos(long total, long ocioso, long iowait) {
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

    public record DiscoContadores(long operacoes, long msEspera, long msOcupado) {
    }

    public record DiscoAtividade(String disco, double ocupacaoPct, Double latenciaMs) {
    }

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

        public Double ocupacaoMaxDiscosPct(Contadores antes) {
            return atividadeDiscos(antes).stream().map(DiscoAtividade::ocupacaoPct).max(Double::compare).orElse(null);
        }

        public long redeErrosDesde(Contadores antes) {
            return Math.max(redeErros - antes.redeErros, 0);
        }
    }

    public record Pressao(Double cpu, Double memoria, Double memoriaTotal, Double io, Double ioTotal) {
    }

    public record Cluster(String nome, List<Integer> nucleos, Long atualMhz, Long maxMhz, Long limiteMhz) {
        public Double limitePct() {
            return maxMhz == null || limiteMhz == null || maxMhz <= 0 ? null : limitar(100.0 * limiteMhz / maxMhz);
        }
    }

    public record Temperatura(String sensor, double celsius) {
    }

    public record EspacoDisco(String nome, String caminho, long total, long usado, double pct, String dispositivo,
                              String modelo, String tipo, Double temperatura, String estadoTemperatura,
                              SaudeSmart saude) {

        public EspacoDisco(String nome, String caminho, long total, long usado, double pct) {
            this(nome, caminho, total, usado, pct, null, null, null, null, null, null);
        }
    }

    public record DiscoSmart(String dispositivo, String modelo, String tipo, Double temperatura, String estado,
                             List<String> dispositivos, SaudeSmart saude) {
    }

    public record MontagemHost(String ponto, String dispositivo, long total, long usado, long disponivel) {
    }

    public record ArquivoSmart(List<DiscoSmart> discos, List<MontagemHost> montagens) {
        static final ArquivoSmart VAZIO = new ArquivoSmart(List.of(), List.of());
    }

    static final Duration SMART_VALIDADE = Duration.ofMinutes(5);

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

    static List<CpuTempos> parseStat(List<String> linhas) {
        List<CpuTempos> cpus = new ArrayList<>();
        for (String linha : linhas) {
            if (!linha.startsWith("cpu")) continue;
            String[] p = linha.trim().split("\\s+");
            long total = 0;
            for (int i = 1; i <= Math.min(8, p.length - 1); i++) total += Long.parseLong(p[i]);
            long idle = Long.parseLong(p[4]);
            long iowait = p.length > 5 ? Long.parseLong(p[5]) : 0;
            cpus.add(new CpuTempos(total, idle + iowait, iowait));
        }
        if (cpus.isEmpty()) throw new IllegalStateException("/proc/stat sem linhas de CPU");
        return cpus;
    }

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

    static Map<String, DiscoContadores> parseDiskstatsPorDisco(List<String> linhas) {
        Map<String, DiscoContadores> discos = new LinkedHashMap<>();
        for (String linha : linhas) {
            String[] p = linha.trim().split("\\s+");
            if (p.length < 13 || !DISCO_INTEIRO.matcher(p[2]).matches()) continue;
            discos.put(p[2], new DiscoContadores(Long.parseLong(p[3]) + Long.parseLong(p[7]),
                    Long.parseLong(p[6]) + Long.parseLong(p[10]), Long.parseLong(p[12])));
        }
        return discos;
    }

    static Double parsePressao(List<String> linhas, String tipo) {
        for (String linha : linhas) {
            if (!linha.startsWith(tipo + " ")) continue;
            Matcher m = MEDIA10.matcher(linha);
            if (m.find()) return Double.parseDouble(m.group(1));
        }
        return null;
    }

    static Double parseCargaDevfreq(String texto) {
        if (texto == null) return null;
        int i = texto.indexOf('@');
        try {
            return limitar(Double.parseDouble((i < 0 ? texto : texto.substring(0, i)).trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String nomeSensor(String tipo) {
        return tipo.trim().replaceAll("[-_ ]?thermal$", "");
    }

    static double limitar(double pct) {
        return Math.max(0, Math.min(100, pct));
    }

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

    private Double lerGpu() {
        Path dir = sys.resolve("class/devfreq");
        if (!Files.isDirectory(dir)) return null;
        for (Path d : ordenados(dir)) {
            if (d.getFileName().toString().toLowerCase().contains("gpu")) return parseCargaDevfreq(textoOuNulo(d.resolve("load")));
        }
        return null;
    }

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

    private List<EspacoDisco> lerDiscos() {
        ArquivoSmart arquivo = lerArquivoSmart(arquivoSmart, OffsetDateTime.now(relogio));
        List<DiscoSmart> smart = arquivo.discos();
        List<EspacoDisco> lista = new ArrayList<>();
        Set<String> medidos = new HashSet<>();
        for (DiscoMonitorado d : discos) {
            MontagemHost m = arquivo.montagens().isEmpty() ? medirLocal(d.caminho()) : montagem(arquivo.montagens(), d.caminho());
            if (m == null || m.total() <= 0) continue;
            long total = m.total(), usado = m.usado(), disponivel = m.disponivel();
            if (!medidos.add(m.dispositivo() != null ? m.dispositivo() : total + ":" + usado + ":" + disponivel)) continue;
            double pct = usado + disponivel > 0 ? limitar(100.0 * usado / (usado + disponivel)) : 0;
            String dispositivo = m.dispositivo();
            DiscoSmart s = dispositivo == null ? null : smart.stream()
                    .filter(x -> x.dispositivos().contains(dispositivo)).findFirst().orElse(null);
            lista.add(s == null
                    ? new EspacoDisco(d.nome(), d.caminho(), total, usado, pct, dispositivo, null, null, null, null, null)
                    : new EspacoDisco(d.nome(), d.caminho(), total, usado, pct, dispositivo, s.modelo(), s.tipo(),
                            s.temperatura(), s.estado(), s.saude()));
        }
        return lista;
    }

    static MontagemHost montagem(List<MontagemHost> montagens, String caminho) {
        String alvo = semBarraFinal(caminho);
        return montagens.stream().filter(m -> semBarraFinal(m.ponto()).equals(alvo)).findFirst().orElse(null);
    }

    private static String semBarraFinal(String p) {
        return p.length() > 1 && p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }

    private static MontagemHost medirLocal(String caminho) {
        File f = new File(caminho);
        long total = f.getTotalSpace();
        if (total <= 0) return null;
        return new MontagemHost(caminho, dispositivo(f.toPath()), total, total - f.getFreeSpace(), f.getUsableSpace());
    }

    static String dispositivo(Path caminho) {
        try {
            Object dev = Files.getAttribute(caminho, "unix:dev");
            return dev instanceof Number n ? majorMinor(n.longValue()) : null;
        } catch (IOException | UnsupportedOperationException | IllegalArgumentException e) {
            return null;
        }
    }

    static String majorMinor(long dev) {
        long major = ((dev >>> 8) & 0xfffL) | ((dev >>> 32) & 0xfffff000L);
        long minor = (dev & 0xffL) | ((dev >>> 12) & 0xffffff00L);
        return major + ":" + minor;
    }

    static List<DiscoSmart> lerSmart(Path arquivo, OffsetDateTime agora) {
        return lerArquivoSmart(arquivo, agora).discos();
    }

    static ArquivoSmart lerArquivoSmart(Path arquivo, OffsetDateTime agora) {
        if (arquivo == null || !Files.isReadable(arquivo)) return ArquivoSmart.VAZIO;
        try {
            Map<?, ?> json = JsonMapper.shared().readValue(arquivo, Map.class);
            OffsetDateTime lidoEm = OffsetDateTime.parse(String.valueOf(json.get("lidoEm")));
            if (lidoEm.isBefore(agora.minus(SMART_VALIDADE))) return ArquivoSmart.VAZIO;
            List<MontagemHost> montagens = new ArrayList<>();
            if (json.get("montagens") instanceof List<?> ms) {
                for (Object o : ms) {
                    if (!(o instanceof Map<?, ?> m) || m.get("ponto") == null) continue;
                    Long total = inteiro(m.get("total")), usado = inteiro(m.get("usado")), disp = inteiro(m.get("disponivel"));
                    if (total == null || usado == null || disp == null) continue;
                    montagens.add(new MontagemHost(textoDe(m.get("ponto")), textoDe(m.get("dispositivo")), total, usado, disp));
                }
            }
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
            return new ArquivoSmart(List.copyOf(lista), List.copyOf(montagens));
        } catch (RuntimeException e) {
            return ArquivoSmart.VAZIO;
        }
    }

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
