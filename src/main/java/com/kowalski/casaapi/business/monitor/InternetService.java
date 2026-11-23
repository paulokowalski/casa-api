package com.kowalski.casaapi.business.monitor;

import com.kowalski.casaapi.servidor.ServidorRepository;
import com.kowalski.casaapi.servidor.LeitorHost;
import com.kowalski.casaapi.servidor.EventosHost;
import com.kowalski.casaapi.api.v1.response.InternetResponse;
import com.kowalski.casaapi.comum.MonitorIndisponivelException;
import com.kowalski.casaapi.config.monitor.MonitorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static com.kowalski.casaapi.servidor.LeitorHost.decimal;
import static com.kowalski.casaapi.servidor.LeitorHost.em;
import static com.kowalski.casaapi.servidor.LeitorHost.inteiro;
import static com.kowalski.casaapi.servidor.LeitorHost.lista;
import static com.kowalski.casaapi.servidor.LeitorHost.local;
import static com.kowalski.casaapi.servidor.LeitorHost.logico;
import static com.kowalski.casaapi.servidor.LeitorHost.texto;

/**
 * Internet da casa: a cada minuto grava em {@code amostra_internet} o que o timer casa-monitor-internet mediu
 * (ping, perda, jitter, DNS, IP público) e em {@code teste_velocidade} os testes novos do casa-monitor-velocidade.
 * Gera eventos (evento_host, tipo internet) quando o IP público muda, quando há perda de pacotes seguida e quando
 * um teste de velocidade falha ou sai bem abaixo do normal.
 */
@Slf4j
@Service
public class InternetService {

    static final int MAX_DIAS_CONSULTA = 366;
    /** Minutos seguidos com perda para registrar (e sem perda para encerrar) um período de perda de pacotes. */
    static final int MINUTOS_PERDA = 3;
    static final double PERDA_MINIMA_PCT = 10;
    /** Download abaixo desta fração da mediana dos últimos testes vira evento. */
    static final double FRACAO_LENTO = 0.5;
    static final int TESTES_COMPARAR = 10;

    private final LeitorInternet leitor;
    private final InternetRepository repository;
    private final ServidorRepository monitorRepository;
    private final MonitorProperties props;

    private volatile boolean tabelasProntas;
    private String ultimoLidoEm;
    /** Teste de velocidade mais novo já gravado: o velocidade.json só muda a cada 4 h, então não regrava a cada minuto. */
    private LocalDateTime ultimoTesteGravado;
    private String ip;
    private boolean ipCarregado;
    private int minutosComPerda;
    private int minutosSemPerda;
    private LocalDateTime perdaDesde;
    private double perdaMax;

    public InternetService(LeitorInternet leitor, InternetRepository repository, ServidorRepository monitorRepository,
                           MonitorProperties props) {
        this.leitor = leitor;
        this.repository = repository;
        this.monitorRepository = monitorRepository;
        this.props = props;
    }

    /** O host grava aos 20 s de cada minuto; lê aos 40 s. */
    @Scheduled(cron = "40 * * * * *")
    public void coletar() {
        coletar(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES), Instant.now());
    }

    synchronized void coletar(LocalDateTime agora, Instant instante) {
        if (!props.habilitado() || !leitor.habilitado() || !prepararTabelas()) return;
        try {
            List<EventosHost.Evento> eventos = new ArrayList<>();
            Map<String, Object> i = leitor.internet();
            if (i != null && !LeitorInternet.desatualizado(i, instante) && !Objects.equals(texto(i.get("lidoEm")), ultimoLidoEm)) {
                ultimoLidoEm = texto(i.get("lidoEm"));
                AmostraInternet a = AmostraInternet.de(agora, i);
                repository.inserir(a);
                eventosAmostra(agora, a, eventos);
            }
            Map<String, Object> v = leitor.velocidade();
            LocalDateTime maisNovo = null;
            if (v != null) {
                List<InternetResponse.TesteVelocidade> lidos = testes(v);
                maisNovo = lidos.stream().map(InternetResponse.TesteVelocidade::testadoEm).max(LocalDateTime::compareTo).orElse(null);
                if (maisNovo != null && !maisNovo.equals(ultimoTesteGravado)) {
                    for (InternetResponse.TesteVelocidade t : repository.inserirTestes(lidos)) eventosTeste(t, eventos);
                } else {
                    maisNovo = null;
                }
            }
            monitorRepository.inserirEventos(eventos);
            if (maisNovo != null) ultimoTesteGravado = maisNovo; // só depois de gravar: se falhar, tenta de novo
        } catch (RuntimeException e) {
            log.warn("Monitoramento: falha na coleta da internet das {}: {}", agora.toLocalTime(), e.getMessage());
        }
    }

    void eventosAmostra(LocalDateTime agora, AmostraInternet a, List<EventosHost.Evento> eventos) {
        if (!ipCarregado) {
            ip = a.ipPublico();
            ipCarregado = true;
        } else if (a.ipPublico() != null && !a.ipPublico().equals(ip)) {
            eventos.add(new EventosHost.Evento(agora, "internet", "ip|" + agora + "|" + a.ipPublico(), EventosHost.INFO,
                    ip == null ? "IP público: " + a.ipPublico()
                            : "IP público mudou de %s para %s".formatted(ip, a.ipPublico())));
            ip = a.ipPublico();
        }

        Double perda = a.perdaInternet();
        if (Boolean.FALSE.equals(a.online()) || perda == null) return; // queda de internet já tem evento próprio
        if (perda >= PERDA_MINIMA_PCT) {
            minutosComPerda++;
            minutosSemPerda = 0;
            perdaMax = Math.max(perdaMax, perda);
            if (minutosComPerda == MINUTOS_PERDA && perdaDesde == null) {
                perdaDesde = agora.minusMinutes(MINUTOS_PERDA - 1L);
                eventos.add(new EventosHost.Evento(perdaDesde, "internet", "perda|" + perdaDesde, EventosHost.ATENCAO,
                        "Perda de pacotes na internet (%.0f%%)".formatted(perda)));
            }
        } else {
            minutosComPerda = 0;
            if (perdaDesde != null && ++minutosSemPerda >= MINUTOS_PERDA) {
                long min = ChronoUnit.MINUTES.between(perdaDesde, agora) - MINUTOS_PERDA + 1;
                eventos.add(new EventosHost.Evento(agora, "internet", "perda-fim|" + perdaDesde, EventosHost.SUCESSO,
                        "Perda de pacotes acabou (durou ~%d min, pico de %.0f%%)".formatted(Math.max(min, 1), perdaMax)));
                perdaDesde = null;
                perdaMax = 0;
            }
            if (perdaDesde == null) perdaMax = 0;
        }
    }

    void eventosTeste(InternetResponse.TesteVelocidade t, List<EventosHost.Evento> eventos) {
        String chave = "velocidade|" + t.testadoEm();
        if (!t.ok()) {
            eventos.add(new EventosHost.Evento(t.testadoEm(), "internet", chave, EventosHost.ATENCAO,
                    "Teste de velocidade falhou" + (t.erro() == null ? "" : ": " + t.erro())));
            return;
        }
        List<Double> antes = repository.downloadsAntes(t.testadoEm(), TESTES_COMPARAR);
        if (antes.size() < 3 || t.downloadMbps() == null) return;
        double mediana = antes.stream().sorted().toList().get(antes.size() / 2);
        if (t.downloadMbps() < mediana * FRACAO_LENTO) {
            eventos.add(new EventosHost.Evento(t.testadoEm(), "internet", chave, EventosHost.ATENCAO,
                    "Internet lenta: download de %.0f Mbit/s (o normal é ~%.0f)".formatted(t.downloadMbps(), mediana)));
        }
    }

    static List<InternetResponse.TesteVelocidade> testes(Map<String, Object> v) {
        return lista(v.get("historico")).stream().map(t -> {
            LocalDateTime quando = local(em(t, "testadoEm"));
            if (quando == null) return null;
            return new InternetResponse.TesteVelocidade(quando, Boolean.TRUE.equals(logico(em(t, "ok"))),
                    decimal(em(t, "downloadMbps")), decimal(em(t, "uploadMbps")), decimal(em(t, "latenciaMs")),
                    decimal(em(t, "jitterMs")), decimal(em(t, "latenciaCargaMs")), inteiro(em(t, "bytes")),
                    texto(em(t, "erro")));
        }).filter(Objects::nonNull).toList();
    }

    @Scheduled(cron = "0 20 3 * * *")
    public void limparAntigas() {
        if (!props.habilitado() || !prepararTabelas()) return;
        try {
            repository.apagarAntesDe(LocalDate.now().minusDays(props.retencaoDias()).atStartOfDay());
        } catch (DataAccessException e) {
            log.warn("Monitoramento: falha ao apagar amostras antigas da internet: {}", e.getMessage());
        }
    }

    /** Leitura atual (internet.json + último teste de velocidade); 503 se o arquivo não existe. */
    public Map<String, Object> agora() {
        if (!leitor.habilitado()) {
            throw new MonitorIndisponivelException("Internet desligada (MONITOR_HOST_ARQUIVO vazio)", null);
        }
        Map<String, Object> r = leitor.lerParaTela();
        if (r == null) {
            throw new MonitorIndisponivelException("Internet indisponível (timer casa-monitor-internet parado?)", null);
        }
        return r;
    }

    public InternetResponse.Dia dia(LocalDate data) {
        LocalDateTime de = data.atStartOfDay();
        LocalDateTime ate = data.plusDays(1).atStartOfDay();
        return historico(() -> new InternetResponse.Dia(data, repository.resumo(de, ate), repository.serie(de, ate),
                repository.testes(de, ate)));
    }

    public List<InternetResponse.ResumoDia> dias(LocalDate de, LocalDate ate) {
        validar(de, ate);
        return historico(() -> repository.porDia(de.atStartOfDay(), ate.plusDays(1).atStartOfDay()).entrySet().stream()
                .map(e -> new InternetResponse.ResumoDia(e.getKey(), e.getValue())).toList());
    }

    public List<InternetResponse.TesteVelocidade> velocidade(LocalDate de, LocalDate ate) {
        validar(de, ate);
        return historico(() -> repository.testes(de.atStartOfDay(), ate.plusDays(1).atStartOfDay()));
    }

    private static void validar(LocalDate de, LocalDate ate) {
        Assert.isTrue(!de.isAfter(ate), "A data inicial deve ser anterior ou igual à final");
        Assert.isTrue(ChronoUnit.DAYS.between(de, ate) < MAX_DIAS_CONSULTA,
                "Consulte no máximo " + MAX_DIAS_CONSULTA + " dias por vez");
    }

    private <T> T historico(Supplier<T> consulta) {
        if (!prepararTabelas()) {
            throw new MonitorIndisponivelException("O banco do monitoramento (MariaDB) está indisponível", null);
        }
        try {
            return consulta.get();
        } catch (DataAccessException e) {
            throw new MonitorIndisponivelException("O banco do monitoramento (MariaDB) está indisponível", e);
        }
    }

    private boolean prepararTabelas() {
        if (tabelasProntas) return true;
        try {
            repository.criarTabelas();
            if (!ipCarregado) {
                ip = repository.ultimoIp();
                ipCarregado = true;
            }
            tabelasProntas = true;
        } catch (DataAccessException e) {
            log.warn("Monitoramento: MariaDB indisponível (internet): {}", e.getMostSpecificCause().getMessage());
        }
        return tabelasProntas;
    }
}
