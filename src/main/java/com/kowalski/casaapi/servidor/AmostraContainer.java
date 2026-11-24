package com.kowalski.casaapi.servidor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record AmostraContainer(LocalDateTime coletadoEm, int intervaloSeg, String nome, String imagem, String estado,
                               String saude, String status, Double cpuPct, Long memUsada, Long memLimite,
                               Double redeRxBps, Double redeTxBps, Double discoLeituraBps, Double discoEscritaBps,
                               Integer processos) {

    public Double memPct() {
        return memUsada == null || memLimite == null || memLimite <= 0 ? null
                : LeitorSistema.limitar(100.0 * memUsada / memLimite);
    }

    public static List<AmostraContainer> de(LocalDateTime coletadoEm, LeitorContainers.Leitura antes,
                                            LeitorContainers.Leitura atual) {
        double seg = Math.max((atual.nanos() - antes.nanos()) / 1e9, 0.001);
        List<AmostraContainer> lista = new ArrayList<>();
        for (LeitorContainers.Container c : atual.containers()) {
            LeitorContainers.Contadores agora = c.contadores();
            LeitorContainers.Container anterior = antes.porId(c.id());
            LeitorContainers.Contadores antigo = anterior == null ? null : anterior.contadores();

            Double cpu = null;
            Double rx = null;
            Double tx = null;
            Double leitura = null;
            Double escrita = null;
            if (agora != null && antigo != null) {
                long dSistema = agora.sistemaNs() - antigo.sistemaNs();
                long dCpu = agora.cpuNs() - antigo.cpuNs();
                cpu = dSistema > 0 && dCpu >= 0 ? LeitorSistema.limitar(100.0 * dCpu / dSistema) : null;
                rx = taxa(antigo.redeRecebidos(), agora.redeRecebidos(), seg);
                tx = taxa(antigo.redeEnviados(), agora.redeEnviados(), seg);
                leitura = taxa(antigo.discoLidos(), agora.discoLidos(), seg);
                escrita = taxa(antigo.discoEscritos(), agora.discoEscritos(), seg);
            }
            lista.add(new AmostraContainer(coletadoEm, (int) Math.round(seg), c.nome(), c.imagem(), c.estado(), c.saude(),
                    LeitorContainers.traduzirStatus(c.status()), cpu,
                    agora == null ? null : agora.memUsada(), agora == null ? null : agora.memLimite(),
                    rx, tx, leitura, escrita, agora == null ? null : agora.processos()));
        }
        return lista;
    }

    private static Double taxa(Long antes, Long depois, double seg) {
        if (antes == null || depois == null || depois < antes) return null;
        return (depois - antes) / seg;
    }
}
