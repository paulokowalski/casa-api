package com.kowalski.casaapi.business.monitor;

import java.time.LocalDateTime;

/** Uma linha de {@code amostra_servidor}: médias do minuto que terminou em {@code coletadoEm}. */
public record Amostra(LocalDateTime coletadoEm, int intervaloSeg,
                      double cpuPct, double iowaitPct,
                      double load1, double load5, double load15, int processos,
                      long memTotal, long memUsada, double memPct,
                      long swapTotal, long swapUsada, double swapPct,
                      Double tempMax, Double tempSoc, Double tempCpu, Double tempGpu,
                      double redeRxBps, double redeTxBps,
                      double discoLeituraBps, double discoEscritaBps) {

    /** Calcula a amostra a partir de duas leituras dos contadores e dos valores instantâneos. */
    public static Amostra de(LocalDateTime coletadoEm, LeitorSistema.Contadores antes, LeitorSistema.Contadores atual,
                             LeitorSistema.Instantaneo inst) {
        double seg = atual.segundosDesde(antes);
        long rx = 0;
        long tx = 0;
        for (var e : atual.rede().entrySet()) {
            LeitorSistema.Trafego anterior = antes.rede().get(e.getKey());
            if (anterior == null) continue;
            rx += Math.max(e.getValue().recebidos() - anterior.recebidos(), 0);
            tx += Math.max(e.getValue().enviados() - anterior.enviados(), 0);
        }
        return new Amostra(coletadoEm, (int) Math.round(seg),
                atual.cpu().usoPct(antes.cpu()), atual.cpu().iowaitPct(antes.cpu()),
                inst.load1(), inst.load5(), inst.load15(), inst.processos(),
                inst.memTotal(), inst.memUsada(), inst.memPct(),
                inst.swapTotal(), inst.swapUsada(), inst.swapPct(),
                inst.temperaturaMax(), inst.temperatura("soc"), inst.temperatura("core", "cpu"), inst.temperatura("gpu"),
                rx / seg, tx / seg,
                Math.max(atual.discoLidos() - antes.discoLidos(), 0) / seg,
                Math.max(atual.discoEscritos() - antes.discoEscritos(), 0) / seg);
    }
}
