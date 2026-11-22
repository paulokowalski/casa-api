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
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
 *       container que reiniciou, aparelho começando ou parando de acessar o servidor pela VPN): comparam a leitura
 *       atual com a anterior guardada em memória. Depois de reiniciar a API a primeira leitura só serve de base.</li>
 * </ul>
 */
public final class EventosHost {

    public static final String INFO = "info";
    public static final String SUCESSO = "sucesso";
    public static final String ATENCAO = "atencao";
    public static final String ERRO = "erro";

    /** Leituras seguidas sem internet antes de registrar a queda (um ping perdido não é queda). */
    static final int LEITURAS_PARA_QUEDA = 2;

    /** Tempo sem tráfego com o servidor para considerar que o aparelho parou de acessar pela VPN. */
    static final Duration VPN_OCIOSO = Duration.ofMinutes(5);
    /** Na primeira leitura (API recém-subida), handshake mais novo que isso = acesso já em andamento. */
    static final Duration VPN_HANDSHAKE_RECENTE = Duration.ofMinutes(3);
    /** Aparelhos adicionados à tailnet há menos que isso geram o evento "aparelho novo". */
    static final Duration VPN_APARELHO_NOVO = Duration.ofDays(7);

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
        private Map<String, AcessoVpn> vpn = new HashMap<>();
        private boolean inicializado;
    }

    /** Contadores de um aparelho da tailnet e o acesso ao servidor em andamento (se houver). */
    private static final class AcessoVpn {
        private long recebidos;
        private long enviados;
        private String handshake;
        private boolean acessando;
        private LocalDateTime inicio;          // nulo = já estava acessando quando a API subiu
        private LocalDateTime ultimoTrafego;
        private long bytes;
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

        acessosVpn(agora, minuto, h, e, eventos);

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

    /**
     * Quem acessa o servidor pela VPN. "Online" no Tailscale é só estar conectado à tailnet; acessar é trocar
     * tráfego com este servidor, que aparece nos bytes e no último handshake WireGuard do aparelho. O acesso começa
     * na primeira leitura com tráfego e termina depois de {@link #VPN_OCIOSO} sem tráfego.
     */
    private static void acessosVpn(LocalDateTime agora, String minuto, Map<String, Object> h, Estado e, List<Evento> eventos) {
        Object ts = em(h, "rede", "tailscale");
        if (!(em(ts, "aparelhos") instanceof List<?> aparelhos)) return; // sem leitura: mantém o que sabia
        String eu = texto(em(ts, "usuario"));
        Map<String, AcessoVpn> atual = new HashMap<>();
        for (Object a : aparelhos) {
            String id = texto(em(a, "id"));
            if (id == null) continue;
            String desc = descricaoAparelho(a, eu);

            LocalDateTime criado = local(em(a, "criado"));
            if (criado != null && criado.isAfter(agora.minus(VPN_APARELHO_NOVO))) {
                eventos.add(new Evento(criado, "tailscale", "tailscale-novo|" + id, ATENCAO,
                        "Aparelho novo na rede Tailscale: " + desc));
            }

            AcessoVpn at = new AcessoVpn();
            at.recebidos = naoNulo(inteiro(em(a, "recebidos")));
            at.enviados = naoNulo(inteiro(em(a, "enviados")));
            at.handshake = texto(em(a, "ultimoHandshake"));
            AcessoVpn ant = e.vpn.get(id);
            if (ant == null && !e.inicializado) {
                // base: um acesso que já estava acontecendo fica com início desconhecido, sem evento de início
                LocalDateTime hs = local(at.handshake);
                if (hs != null && !hs.isBefore(agora.minus(VPN_HANDSHAKE_RECENTE))) {
                    at.acessando = true;
                    at.ultimoTrafego = agora;
                }
                atual.put(id, at);
                continue;
            }
            if (ant == null) ant = new AcessoVpn(); // aparelho que entrou agora na tailnet: contadores partem do zero
            at.acessando = ant.acessando;
            at.inicio = ant.inicio;
            at.ultimoTrafego = ant.ultimoTrafego;
            at.bytes = ant.bytes;

            // contador menor que o anterior = o Tailscale reiniciou o par (ex.: ficou ocioso); conta do zero
            long delta = (at.recebidos >= ant.recebidos ? at.recebidos - ant.recebidos : at.recebidos)
                    + (at.enviados >= ant.enviados ? at.enviados - ant.enviados : at.enviados);
            boolean trafego = delta > 0 || (at.handshake != null && !at.handshake.equals(ant.handshake));
            if (trafego) {
                if (!at.acessando) {
                    at.acessando = true;
                    at.inicio = agora;
                    at.bytes = 0;
                    eventos.add(estado(agora, minuto, "tailscale", "inicio|" + id, INFO,
                            desc + " começou a acessar o servidor pela VPN"));
                }
                at.bytes += delta;
                at.ultimoTrafego = agora;
            } else if (at.acessando && at.ultimoTrafego != null && !at.ultimoTrafego.isAfter(agora.minus(VPN_OCIOSO))) {
                String detalhe = at.inicio == null
                        ? "%s trocados desde que a API subiu".formatted(tamanho(at.bytes))
                        : "%d min, %s trocados".formatted(
                                Math.max(Duration.between(at.inicio, at.ultimoTrafego).toMinutes(), 1), tamanho(at.bytes));
                eventos.add(estado(agora, minuto, "tailscale", "fim|" + id, INFO,
                        "%s parou de acessar o servidor pela VPN (%s)".formatted(desc, detalhe)));
                at.acessando = false;
                at.inicio = null;
                at.bytes = 0;
            }
            atual.put(id, at);
        }
        e.vpn = atual;
    }

    /** "ipad157 (iOS · 100.71.41.49)", com o dono quando o aparelho é de outra conta. */
    private static String descricaoAparelho(Object a, String eu) {
        String nome = Objects.toString(texto(em(a, "nome")), "aparelho sem nome");
        List<String> partes = new ArrayList<>();
        if (texto(em(a, "so")) != null && !texto(em(a, "so")).isBlank()) partes.add(texto(em(a, "so")));
        if (texto(em(a, "ip")) != null) partes.add(texto(em(a, "ip")));
        String dono = texto(em(a, "dono"));
        if (dono != null && !dono.equals(eu)) partes.add("de " + dono);
        return partes.isEmpty() ? nome : nome + " (" + String.join(" · ", partes) + ")";
    }

    private static long naoNulo(Long v) {
        return v == null ? 0 : v;
    }

    static String tamanho(long bytes) {
        String[] unidades = {"B", "KB", "MB", "GB", "TB"};
        double v = bytes;
        int i = 0;
        while (v >= 1024 && i < unidades.length - 1) {
            v /= 1024;
            i++;
        }
        return i == 0 ? bytes + " B" : String.format(Locale.forLanguageTag("pt-BR"), "%.1f %s", v, unidades[i]);
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
