package com.kowalski.casaapi.business.monitor;

import com.kowalski.casaapi.config.monitor.MonitorProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê o status do host gravado a cada minuto pelo timer {@code casa-monitor-host} (backup, serviços, montagens,
 * alertas do monitor, kernel, atualizações, rede, logins SSH, NPU e Docker). A API não tem acesso a nada disso
 * de dentro do container; só lê o JSON, montado só-leitura.
 */
@Component
public class LeitorHost {

    /** O timer roda a cada minuto; acima disso o host parou de atualizar. */
    static final Duration VALIDADE = Duration.ofMinutes(5);

    private final Path arquivo;

    public LeitorHost(MonitorProperties props) {
        this.arquivo = props.hostArquivo() == null ? null : Path.of(props.hostArquivo());
    }

    public boolean habilitado() {
        return arquivo != null;
    }

    /** O JSON do host como mapa, ou nulo se o arquivo não existe ou é inválido. */
    public Map<String, Object> ler() {
        if (arquivo == null || !Files.isReadable(arquivo)) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = JsonMapper.shared().readValue(arquivo, Map.class);
            return json;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** O JSON com {@code desatualizado} (arquivo com mais de 5 minutos), para a tela. */
    public Map<String, Object> lerParaTela() {
        Map<String, Object> json = ler();
        if (json == null) return null;
        Map<String, Object> r = new LinkedHashMap<>(json);
        r.put("desatualizado", desatualizado(json, Instant.now()));
        return r;
    }

    static boolean desatualizado(Map<String, Object> json, Instant agora) {
        OffsetDateTime lido = data(json.get("lidoEm"));
        return lido == null || lido.toInstant().isBefore(agora.minus(VALIDADE));
    }

    // ---- navegação no JSON ----

    static Object em(Object raiz, String... caminho) {
        Object atual = raiz;
        for (String c : caminho) {
            if (!(atual instanceof Map<?, ?> m)) return null;
            atual = m.get(c);
        }
        return atual;
    }

    static Double decimal(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    static Long inteiro(Object o) {
        return o instanceof Number n ? n.longValue() : null;
    }

    static Boolean logico(Object o) {
        return o instanceof Boolean b ? b : null;
    }

    static String texto(Object o) {
        return o == null ? null : o.toString();
    }

    static List<?> lista(Object o) {
        return o instanceof List<?> l ? l : List.of();
    }

    static OffsetDateTime data(Object o) {
        if (o == null) return null;
        try {
            return OffsetDateTime.parse(o.toString());
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Data do JSON no fuso do servidor (o banco guarda DATETIME local). */
    static LocalDateTime local(Object o) {
        OffsetDateTime d = data(o);
        return d == null ? null : d.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
