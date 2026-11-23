package com.kowalski.casaapi.business.monitor;

import com.kowalski.casaapi.servidor.LeitorHost;
import com.kowalski.casaapi.config.monitor.MonitorProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lê a internet da casa gravada no host, na mesma pasta do status do host ({@code monitor.host-arquivo}):
 * {@code internet.json} (timer casa-monitor-internet, a cada minuto: ping, DNS, IP público, Pi-hole, interface) e
 * {@code velocidade.json} (timer casa-monitor-velocidade, a cada 4 horas: último teste e os mais recentes).
 * Fontes em config/internet.
 */
@Component
public class LeitorInternet {

    /** O timer roda a cada minuto; acima disso o host parou de atualizar. */
    static final Duration VALIDADE = Duration.ofMinutes(3);

    private final Path internet;
    private final Path velocidade;

    @Autowired
    public LeitorInternet(MonitorProperties props) {
        Path host = props.hostArquivo() == null ? null : Path.of(props.hostArquivo());
        this.internet = host == null ? null : host.resolveSibling("internet.json");
        this.velocidade = host == null ? null : host.resolveSibling("velocidade.json");
    }

    LeitorInternet(Path internet, Path velocidade) {
        this.internet = internet;
        this.velocidade = velocidade;
    }

    public boolean habilitado() {
        return internet != null;
    }

    public Map<String, Object> internet() {
        return ler(internet);
    }

    public Map<String, Object> velocidade() {
        return ler(velocidade);
    }

    /** Internet e último teste de velocidade juntos, com {@code desatualizado}, para a tela. */
    public Map<String, Object> lerParaTela() {
        Map<String, Object> i = internet();
        if (i == null) return null;
        Map<String, Object> r = new LinkedHashMap<>(i);
        r.put("desatualizado", desatualizado(i, Instant.now()));
        Map<String, Object> v = velocidade();
        r.put("velocidade", v == null ? null : v.get("ultimo"));
        return r;
    }

    static boolean desatualizado(Map<String, Object> json, Instant agora) {
        OffsetDateTime lido = LeitorHost.data(json.get("lidoEm"));
        return lido == null || lido.toInstant().isBefore(agora.minus(VALIDADE));
    }

    private static Map<String, Object> ler(Path arquivo) {
        if (arquivo == null || !Files.isReadable(arquivo)) return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = JsonMapper.shared().readValue(arquivo, Map.class);
            return json;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
