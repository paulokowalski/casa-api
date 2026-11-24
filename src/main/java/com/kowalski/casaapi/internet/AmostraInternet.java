package com.kowalski.casaapi.internet;

import java.time.LocalDateTime;
import java.util.Map;

import static com.kowalski.casaapi.servidor.LeitorHost.decimal;
import static com.kowalski.casaapi.servidor.LeitorHost.em;
import static com.kowalski.casaapi.servidor.LeitorHost.inteiro;
import static com.kowalski.casaapi.servidor.LeitorHost.lista;
import static com.kowalski.casaapi.servidor.LeitorHost.logico;
import static com.kowalski.casaapi.servidor.LeitorHost.texto;

public record AmostraInternet(LocalDateTime coletadoEm, Boolean online,
                              Ping roteador, Ping modem, Ping cloudflare, Ping google,
                              Dns pihole, Dns dnsCloudflare, Dns dnsGoogle,
                              String ipPublico, Double piholeBloqueadasPct, Long piholeConsultas24h) {

    public record Ping(Double mediaMs, Double maxMs, Double jitterMs, Double perdaPct) {
        static final Ping VAZIO = new Ping(null, null, null, null);
    }

    public record Dns(Boolean ok, Double tempoMs) {
        static final Dns VAZIO = new Dns(null, null);
    }

    public static AmostraInternet de(LocalDateTime coletadoEm, Map<String, Object> i) {
        return new AmostraInternet(coletadoEm, logico(i.get("online")),
                ping(i, "roteador"), ping(i, "modem"), ping(i, "cloudflare"), ping(i, "google"),
                dns(i, "pihole"), dns(i, "cloudflare"), dns(i, "google"),
                texto(em(i, "publico", "ip")),
                decimal(em(i, "pihole", "bloqueadasPct")), inteiro(em(i, "pihole", "consultas24h")));
    }

    static Object porId(Object lista, String id) {
        return lista(lista).stream().filter(p -> id.equals(texto(em(p, "id")))).findFirst().orElse(null);
    }

    private static Ping ping(Map<String, Object> i, String id) {
        Object p = porId(i.get("ping"), id);
        if (p == null) return Ping.VAZIO;
        return new Ping(decimal(em(p, "mediaMs")), decimal(em(p, "maxMs")), decimal(em(p, "jitterMs")),
                decimal(em(p, "perdaPct")));
    }

    private static Dns dns(Map<String, Object> i, String id) {
        Object d = porId(i.get("dns"), id);
        if (d == null) return Dns.VAZIO;
        return new Dns(logico(em(d, "ok")), decimal(em(d, "tempoMs")));
    }

    public Double perdaInternet() {
        if (cloudflare.perdaPct() == null) return google.perdaPct();
        if (google.perdaPct() == null) return cloudflare.perdaPct();
        return Math.min(cloudflare.perdaPct(), google.perdaPct());
    }
}
