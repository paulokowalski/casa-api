package com.kowalski.casaapi.business.monitor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.kowalski.casaapi.business.monitor.LeitorHost.em;
import static com.kowalski.casaapi.business.monitor.LeitorHost.inteiro;
import static com.kowalski.casaapi.business.monitor.LeitorHost.lista;
import static com.kowalski.casaapi.business.monitor.LeitorHost.local;
import static com.kowalski.casaapi.business.monitor.LeitorHost.logico;
import static com.kowalski.casaapi.business.monitor.LeitorHost.texto;

/**
 * Transforma leituras do status do host em eventos para o histórico ({@code evento_host}).
 * <p>
 * Dois tipos de evento:
 * <ul>
 *   <li><b>com data própria</b> (logins SSH, execuções do backup, linhas do kernel, boot): a chave é derivada do
 *       próprio fato, então reler o mesmo arquivo ou reiniciar a API não duplica nada ({@code INSERT IGNORE});</li>
 *   <li><b>mudanças de estado</b> (alerta novo/resolvido, serviço que falhou, disco desmontado, internet fora,
 *       container que reiniciou): comparam a leitura atual com a anterior guardada em memória. Depois de reiniciar
 *       a API a primeira leitura só serve de base.</li>
 * </ul>
 */
public final class EventosHost {

    public static final String INFO = "info";
    public static final String SUCESSO = "sucesso";
    public static final String ATENCAO = "atencao";
    public static final String ERRO = "erro";

    /** Leituras seguidas sem internet antes de registrar a queda (um ping perdido não é queda). */
    static final int LEITURAS_PARA_QUEDA = 2;

    public record Evento(LocalDateTime quando, String tipo, String chave, String nivel, String mensagem) {
    }

    /** O que precisa ser lembrado entre uma leitura e outra. */
    public static final class Estado {
        private Set<String> alertas;
        private Map<String, Boolean> servicos;
        private Map<String, Boolean> montagens;
        private Map<String, Long> reinicios;
        private Boolean tailscale;
        private Boolean reboot;
        private int leiturasSemInternet;
        private LocalDateTime internetCaiuEm;
        private boolean inicializado;
    }

    private EventosHost() {
    }

    public static List<Evento> calcular(LocalDateTime agora, Map<String, Object> h, Estado estado) {
        List<Evento> eventos = new ArrayList<>();
        comDataPropria(h, eventos);
        mudancas(agora, h, estado, eventos);
        return eventos;
    }

    /** Boot do servidor (btime do /proc/stat). */
    public static Evento boot(long bootEpoch) {
        LocalDateTime quando = LocalDateTime.ofInstant(Instant.ofEpochSecond(bootEpoch), ZoneId.systemDefault());
        return new Evento(quando, "boot", "boot|" + bootEpoch, ATENCAO, "Servidor ligado (reinício)");
    }

    // ---- eventos com data própria ----

    private static void comDataPropria(Map<String, Object> h, List<Evento> eventos) {
        for (Object l : lista(h.get("loginsSsh"))) {
            LocalDateTime quando = local(em(l, "quando"));
            if (quando == null) continue;
            String origem = texto(em(l, "origem"));
            eventos.add(new Evento(quando, "ssh", "ssh|" + texto(em(l, "quando")) + "|" + origem, INFO,
                    "Login SSH de %s vindo de %s (%s)".formatted(texto(em(l, "usuario")), origem, texto(em(l, "metodo")))));
        }

        Object exec = em(h, "backup", "ultimaExecucao");
        LocalDateTime inicio = local(em(exec, "inicio"));
        LocalDateTime fim = local(em(exec, "fim"));
        if (inicio != null && fim != null && !fim.isBefore(inicio) && !Boolean.TRUE.equals(logico(em(exec, "rodandoAgora")))) {
            boolean ok = Boolean.TRUE.equals(logico(em(exec, "sucesso")));
            long minutos = Duration.between(inicio, fim).toMinutes();
            Long falhas = inteiro(em(exec, "falhas"));
            eventos.add(new Evento(fim, "backup", "backup|" + texto(em(exec, "inicio")), ok ? SUCESSO : ERRO,
                    ok ? "Backup concluído em %d min".formatted(minutos)
                            : "Backup terminou com %s falha(s) — veja journalctl -u backup-servidor"
                            .formatted(falhas == null ? "?" : falhas)));
        }

        for (Object o : lista(em(h, "kernel", "ultimos"))) {
            String linha = texto(o);
            if (linha == null || linha.isBlank()) continue;
            int espaco = linha.indexOf(' ');
            LocalDateTime quando = espaco > 0 ? local(linha.substring(0, espaco)) : null;
            if (quando == null) continue;
            String msg = linha.substring(espaco + 1);
            int k = msg.indexOf("kernel: ");
            if (k >= 0) msg = msg.substring(k + 8);
            eventos.add(new Evento(quando, "kernel", "kernel|" + hash(linha), ERRO, "Kernel: " + msg));
        }
    }

    // ---- mudanças de estado ----

    private static void mudancas(LocalDateTime agora, Map<String, Object> h, Estado e, List<Evento> eventos) {
        String minuto = agora.truncatedTo(ChronoUnit.MINUTES).toString();

        Set<String> alertas = new LinkedHashSet<>();
        lista(em(h, "alertas", "alertados")).forEach(a -> alertas.add(texto(a)));

        Map<String, Boolean> servicos = new LinkedHashMap<>();
        for (Object s : lista(h.get("servicos"))) {
            if (!Boolean.TRUE.equals(logico(em(s, "instalado")))) continue;
            servicos.put(texto(em(s, "unidade")), Boolean.TRUE.equals(logico(em(s, "ok"))));
        }
        lista(h.get("unidadesFalhas")).forEach(u -> servicos.put(texto(u), false));

        Map<String, Boolean> montagens = new LinkedHashMap<>();
        for (Object m : lista(em(h, "montagens", "discos"))) {
            montagens.put(texto(em(m, "ponto")), Boolean.TRUE.equals(logico(em(m, "montado")))
                    && !Boolean.TRUE.equals(logico(em(m, "somenteLeitura"))));
        }

        Map<String, Long> reinicios = new LinkedHashMap<>();
        Map<String, Boolean> oom = new LinkedHashMap<>();
        for (Object c : lista(em(h, "docker", "containers"))) {
            Long r = inteiro(em(c, "reinicios"));
            if (r != null) reinicios.put(texto(em(c, "nome")), r);
            oom.put(texto(em(c, "nome")), Boolean.TRUE.equals(logico(em(c, "oomKilled"))));
        }

        Boolean internet = logico(em(h, "rede", "internet", "online"));
        Boolean tailscale = logico(em(h, "rede", "tailscale", "online"));
        Boolean reboot = logico(em(h, "atualizacoes", "reinicioNecessario"));

        if (e.inicializado) {
            for (String a : alertas) {
                if (!e.alertas.contains(a)) eventos.add(estado(agora, minuto, "alerta", a, ERRO, "Alerta: " + a));
            }
            for (String a : e.alertas) {
                if (!alertas.contains(a)) eventos.add(estado(agora, minuto, "alerta", a, SUCESSO, "Resolvido: " + a));
            }
            servicos.forEach((u, ok) -> {
                Boolean antes = e.servicos.get(u);
                if (Boolean.TRUE.equals(antes) && !ok) eventos.add(estado(agora, minuto, "servico", u, ERRO, "Serviço com falha: " + u));
                if (Boolean.FALSE.equals(antes) && ok) eventos.add(estado(agora, minuto, "servico", u, SUCESSO, "Serviço normalizado: " + u));
            });
            montagens.forEach((m, ok) -> {
                Boolean antes = e.montagens.get(m);
                if (Boolean.TRUE.equals(antes) && !ok) eventos.add(estado(agora, minuto, "disco", m, ERRO, "Disco desmontado ou só leitura: " + m));
                if (Boolean.FALSE.equals(antes) && ok) eventos.add(estado(agora, minuto, "disco", m, SUCESSO, "Disco montado de novo: " + m));
            });
            reinicios.forEach((c, r) -> {
                Long antes = e.reinicios.get(c);
                if (antes != null && r > antes) {
                    String porque = Boolean.TRUE.equals(oom.get(c)) ? " (sem memória: OOM)" : "";
                    eventos.add(estado(agora, minuto, "container", c, ATENCAO,
                            "Container %s reiniciou sozinho%s — %d reinício(s) no total".formatted(c, porque, r)));
                }
            });
            if (Boolean.TRUE.equals(e.tailscale) && Boolean.FALSE.equals(tailscale)) {
                eventos.add(estado(agora, minuto, "tailscale", "", ATENCAO, "Tailscale offline (acesso remoto fora)"));
            }
            if (Boolean.FALSE.equals(e.tailscale) && Boolean.TRUE.equals(tailscale)) {
                eventos.add(estado(agora, minuto, "tailscale", "", SUCESSO, "Tailscale online de novo"));
            }
            if (!Boolean.TRUE.equals(e.reboot) && Boolean.TRUE.equals(reboot)) {
                eventos.add(estado(agora, minuto, "reboot", "", ATENCAO, "Atualizações pedem reinício do servidor"));
            }
        }

        // Internet: só registra a queda depois de leituras seguidas sem resposta
        if (Boolean.FALSE.equals(internet)) {
            e.leiturasSemInternet++;
            if (e.leiturasSemInternet == LEITURAS_PARA_QUEDA) {
                e.internetCaiuEm = agora.minusMinutes(LEITURAS_PARA_QUEDA - 1L);
                eventos.add(estado(e.internetCaiuEm, e.internetCaiuEm.truncatedTo(ChronoUnit.MINUTES).toString(),
                        "internet", "", ERRO, "Internet fora do ar"));
            }
        } else if (Boolean.TRUE.equals(internet)) {
            if (e.internetCaiuEm != null) {
                long min = Math.max(Duration.between(e.internetCaiuEm, agora).toMinutes(), 1);
                eventos.add(estado(agora, minuto, "internet", "", SUCESSO, "Internet voltou (fora por ~%d min)".formatted(min)));
            }
            e.leiturasSemInternet = 0;
            e.internetCaiuEm = null;
        }

        e.alertas = alertas;
        e.servicos = servicos;
        e.montagens = montagens;
        e.reinicios = reinicios;
        e.tailscale = tailscale;
        e.reboot = reboot;
        e.inicializado = true;
    }

    private static Evento estado(LocalDateTime quando, String minuto, String tipo, String sujeito, String nivel, String msg) {
        return new Evento(quando, tipo, tipo + "|" + nivel + "|" + minuto + "|" + hash(Objects.toString(sujeito, "")), nivel, msg);
    }

    static String hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
