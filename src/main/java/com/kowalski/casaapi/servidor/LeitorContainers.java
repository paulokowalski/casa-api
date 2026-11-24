package com.kowalski.casaapi.servidor;

import com.kowalski.casaapi.config.monitor.MonitorProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LeitorContainers {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<Map<String, Object>> OBJETO = new ParameterizedTypeReference<>() { };

    private final RestClient http;

    public LeitorContainers(MonitorProperties props) {
        if (!props.containersHabilitados()) {
            this.http = null;
            return;
        }
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder().baseUrl(props.dockerUrl()).requestFactory(fabrica).build();
    }

    public record Contadores(long cpuNs, long sistemaNs, Long memUsada, Long memLimite,
                             Long redeRecebidos, Long redeEnviados, Long discoLidos, Long discoEscritos, Integer processos) {
    }

    public record Container(String id, String nome, String imagem, String estado, String status, String saude,
                            Contadores contadores) {

        public boolean rodando() {
            return "running".equals(estado);
        }
    }

    public record Leitura(long nanos, List<Container> containers) {

        public Container porId(String id) {
            return containers.stream().filter(c -> c.id().equals(id)).findFirst().orElse(null);
        }
    }

    public boolean habilitado() {
        return http != null;
    }

    public Leitura ler() {
        if (http == null) return new Leitura(System.nanoTime(), List.of());
        List<Map<String, Object>> lista = http.get().uri("/containers/json?all=true").retrieve().body(LISTA);
        List<Container> containers = new ArrayList<>();
        for (Map<String, Object> c : lista == null ? List.<Map<String, Object>>of() : lista) {
            String id = texto(c.get("Id"));
            String estado = texto(c.get("State"));
            Contadores contadores = null;
            if ("running".equals(estado)) {
                try {
                    contadores = parseStats(http.get().uri("/containers/{id}/stats?stream=false&one-shot=true", id)
                            .retrieve().body(OBJETO));
                } catch (RestClientException e) {
                }
            }
            String status = texto(c.get("Status"));
            containers.add(new Container(id, nome(c.get("Names")), texto(c.get("Image")), estado, status,
                    saude(status), contadores));
        }
        containers.sort(Comparator.comparing(Container::nome));
        return new Leitura(System.nanoTime(), containers);
    }

    @SuppressWarnings("unchecked")
    static Contadores parseStats(Map<String, Object> s) {
        if (s == null) return null;
        Long cpu = longo(caminho(s, "cpu_stats", "cpu_usage", "total_usage"));
        Long sistema = longo(caminho(s, "cpu_stats", "system_cpu_usage"));
        if (cpu == null || sistema == null) return null;

        Long uso = longo(caminho(s, "memory_stats", "usage"));
        Long limite = longo(caminho(s, "memory_stats", "limit"));
        Long inativo = longo(caminho(s, "memory_stats", "stats", "inactive_file"));
        if (inativo == null) inativo = longo(caminho(s, "memory_stats", "stats", "total_inactive_file"));
        Long memUsada = uso == null ? null : Math.max(uso - (inativo == null ? 0 : inativo), 0);

        Long rx = null;
        Long tx = null;
        if (s.get("networks") instanceof Map<?, ?> redes) {
            rx = 0L;
            tx = 0L;
            for (Object r : redes.values()) {
                if (!(r instanceof Map<?, ?> rede)) continue;
                rx += valorOuZero(rede.get("rx_bytes"));
                tx += valorOuZero(rede.get("tx_bytes"));
            }
        }

        Long lidos = null;
        Long escritos = null;
        if (caminho(s, "blkio_stats", "io_service_bytes_recursive") instanceof List<?> io) {
            lidos = 0L;
            escritos = 0L;
            for (Object item : io) {
                if (!(item instanceof Map<?, ?> m)) continue;
                String op = texto(m.get("op")).toLowerCase();
                if (op.equals("read")) lidos += valorOuZero(m.get("value"));
                else if (op.equals("write")) escritos += valorOuZero(m.get("value"));
            }
        }

        Long pids = longo(caminho(s, "pids_stats", "current"));
        return new Contadores(cpu, sistema, memUsada, limite, rx, tx, lidos, escritos,
                pids == null ? null : pids.intValue());
    }

    private static final Pattern SAUDE = Pattern.compile("\\((healthy|unhealthy|health: starting)\\)");

    static String saude(String status) {
        if (status == null) return null;
        Matcher m = SAUDE.matcher(status);
        if (!m.find()) return null;
        return switch (m.group(1)) {
            case "healthy" -> "saudavel";
            case "unhealthy" -> "doente";
            default -> "iniciando";
        };
    }

    private static final List<String[]> TRADUCOES = List.of(
            new String[]{"\\s*\\((healthy|unhealthy|health: starting)\\)", ""},
            new String[]{"^Up ", "ligado há "},
            new String[]{"^Exited \\((-?\\d+)\\) (.+) ago$", "parado (código $1) há $2"},
            new String[]{"^Restarting \\((-?\\d+)\\) (.+) ago$", "reiniciando (código $1), há $2"},
            new String[]{"^Created$", "criado"},
            new String[]{" \\(Paused\\)$", " (pausado)"},
            new String[]{"Less than a second", "menos de 1 segundo"},
            new String[]{"About a minute", "cerca de 1 minuto"},
            new String[]{"About an hour", "cerca de 1 hora"},
            new String[]{"\\b1 second\\b", "1 segundo"},
            new String[]{"\\b(\\d+) seconds\\b", "$1 segundos"},
            new String[]{"\\b(\\d+) minutes\\b", "$1 minutos"},
            new String[]{"\\b(\\d+) hours\\b", "$1 horas"},
            new String[]{"\\b(\\d+) days\\b", "$1 dias"},
            new String[]{"\\b(\\d+) weeks\\b", "$1 semanas"},
            new String[]{"\\b(\\d+) months\\b", "$1 meses"},
            new String[]{"\\b(\\d+) years\\b", "$1 anos"},
            new String[]{"\\ban? (hour|minute)\\b", "1 $1"});

    static String traduzirStatus(String status) {
        if (status == null) return null;
        String s = status.trim();
        for (String[] t : TRADUCOES) s = s.replaceAll(t[0], t[1]);
        return s;
    }

    private static String nome(Object nomes) {
        if (nomes instanceof List<?> l && !l.isEmpty()) return texto(l.get(0)).replaceFirst("^/", "");
        return "?";
    }

    private static Object caminho(Map<String, Object> raiz, String... chaves) {
        Object atual = raiz;
        for (String chave : chaves) {
            if (!(atual instanceof Map<?, ?> m)) return null;
            atual = m.get(chave);
        }
        return atual;
    }

    private static Long longo(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }

    private static long valorOuZero(Object o) {
        return o instanceof Number n ? n.longValue() : 0;
    }

    private static String texto(Object o) {
        return o == null ? "" : o.toString();
    }
}
