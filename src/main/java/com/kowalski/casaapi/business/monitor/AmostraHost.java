package com.kowalski.casaapi.business.monitor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.kowalski.casaapi.business.monitor.LeitorHost.decimal;
import static com.kowalski.casaapi.business.monitor.LeitorHost.em;
import static com.kowalski.casaapi.business.monitor.LeitorHost.inteiro;
import static com.kowalski.casaapi.business.monitor.LeitorHost.lista;
import static com.kowalski.casaapi.business.monitor.LeitorHost.logico;

/**
 * Uma linha de {@code amostra_host}: o que importa do status do host num minuto.
 *
 * @param backupIdadeSeg idade do snapshot mais antigo entre as cópias (a cópia mais atrasada manda)
 * @param servicosFalhos serviços/timers instalados que não estão ok, mais as unidades em falha do systemd
 */
public record AmostraHost(LocalDateTime coletadoEm,
                          Boolean internetOk, Double internetMs, Double gatewayMs,
                          Boolean dnsOk, Double dnsMs, Boolean tailscaleOnline, Double npuPct,
                          int alertas, int pendentes, int servicosFalhos, int discosDesmontados,
                          Long kernelEventos24h, Long aptPendentes, Long aptSeguranca, Boolean reinicioNecessario,
                          Long backupIdadeSeg, Boolean backupOk, Long containersReinicios) {

    public static AmostraHost de(LocalDateTime coletadoEm, Map<String, Object> h) {
        int servicosFalhos = (int) lista(h.get("servicos")).stream()
                .filter(s -> Boolean.TRUE.equals(logico(em(s, "instalado"))) && !Boolean.TRUE.equals(logico(em(s, "ok"))))
                .count() + lista(h.get("unidadesFalhas")).size();
        int desmontados = (int) lista(em(h, "montagens", "discos")).stream()
                .filter(d -> Boolean.FALSE.equals(logico(em(d, "montado")))).count();
        Long backupIdade = lista(em(h, "backup", "repositorios")).stream()
                .map(r -> inteiro(em(r, "idadeSegundos"))).filter(Objects::nonNull).max(Long::compare).orElse(null);
        List<Double> npu = lista(em(h, "npu", "nucleosPct")).stream().map(LeitorHost::decimal).filter(Objects::nonNull).toList();
        Long reinicios = lista(em(h, "docker", "containers")).stream()
                .map(c -> inteiro(em(c, "reinicios"))).filter(Objects::nonNull).reduce(Long::sum).orElse(null);
        return new AmostraHost(coletadoEm,
                logico(em(h, "rede", "internet", "online")), decimal(em(h, "rede", "internet", "latenciaMs")),
                decimal(em(h, "rede", "gateway", "latenciaMs")),
                logico(em(h, "rede", "dns", "ok")), decimal(em(h, "rede", "dns", "tempoMs")),
                logico(em(h, "rede", "tailscale", "online")),
                npu.isEmpty() ? null : npu.stream().mapToDouble(Double::doubleValue).average().orElse(0),
                lista(em(h, "alertas", "alertados")).size(), lista(em(h, "alertas", "pendentes")).size(),
                servicosFalhos, desmontados,
                inteiro(em(h, "kernel", "eventos24h")), inteiro(em(h, "atualizacoes", "pacotesPendentes")),
                inteiro(em(h, "atualizacoes", "pacotesSeguranca")), logico(em(h, "atualizacoes", "reinicioNecessario")),
                backupIdade, logico(em(h, "backup", "ultimaExecucao", "sucesso")), reinicios);
    }
}
