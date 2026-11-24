package com.kowalski.casaapi.config.monitor;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "monitor")
public record MonitorProperties(Boolean habilitado,
                                Integer retencaoDias,
                                String procPath,
                                String sysPath,
                                String hostnameArquivo,
                                List<String> discos,
                                String dockerUrl,
                                Integer retencaoContainersDias,
                                String smartArquivo,
                                String hostArquivo,
                                Datasource datasource) {

    public MonitorProperties {
        habilitado = habilitado == null || habilitado;
        retencaoDias = retencaoDias == null || retencaoDias < 1 ? 365 : retencaoDias;
        procPath = vazio(procPath) ? "/proc" : procPath;
        sysPath = vazio(sysPath) ? "/sys" : sysPath;
        hostnameArquivo = vazio(hostnameArquivo) ? "/etc/hostname" : hostnameArquivo;
        discos = discos == null || discos.isEmpty() ? List.of("Sistema=/") : List.copyOf(discos);
        dockerUrl = vazio(dockerUrl) ? null : dockerUrl.trim().replaceAll("/+$", "");
        retencaoContainersDias = retencaoContainersDias == null || retencaoContainersDias < 1 ? 90 : retencaoContainersDias;
        smartArquivo = vazio(smartArquivo) ? null : smartArquivo.trim();
        hostArquivo = vazio(hostArquivo) ? null : hostArquivo.trim();
        datasource = datasource == null ? new Datasource(null, null, null) : datasource;
    }

    public boolean containersHabilitados() {
        return dockerUrl != null;
    }

    public record Datasource(String url, String username, String password) {
    }

    public record DiscoMonitorado(String nome, String caminho) {
    }

    public List<DiscoMonitorado> discosMonitorados() {
        return discos.stream()
                .map(String::trim)
                .filter(d -> !d.isEmpty())
                .map(d -> {
                    int i = d.indexOf('=');
                    return i < 0 ? new DiscoMonitorado(d, d) : new DiscoMonitorado(d.substring(0, i).trim(), d.substring(i + 1).trim());
                })
                .toList();
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}
