package com.kowalski.casaapi.business.monitor;

import com.kowalski.casaapi.api.v1.response.ServidorAgoraResponse;
import com.kowalski.casaapi.api.v1.response.ServidorContainerResponse;
import com.kowalski.casaapi.api.v1.response.ServidorDiaResponse;
import com.kowalski.casaapi.api.v1.response.ServidorDiscoResponse;
import com.kowalski.casaapi.api.v1.response.ServidorResumoDiaResponse;
import com.kowalski.casaapi.business.exception.MonitorIndisponivelException;
import com.kowalski.casaapi.config.monitor.MonitorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Monitoramento de saúde do servidor.
 * <p>
 * A cada minuto lê os contadores do kernel e grava em {@code amostra_servidor} as médias do minuto que passou
 * (CPU, rede e disco são a diferença entre a leitura atual e a anterior). O espaço dos discos muda devagar e é
 * gravado a cada 10 minutos. Com {@code monitor.docker-url} configurado, grava também uma amostra por container
 * em {@code amostra_container}. Falhas na coleta só geram um aviso no log: a API continua no ar.
 */
@Slf4j
@Service
public class MonitorService {

    static final int MAX_DIAS_CONSULTA = 366;
    private static final long MEIO_SEGUNDO = 500;
    private static final int MINUTOS_ENTRE_DISCOS = 10;

    private final LeitorSistema leitor;
    private final LeitorContainers leitorContainers;
    private final MonitorRepository repository;
    private final MonitorProperties props;

    private volatile LeitorSistema.Contadores anterior;
    private volatile LeitorContainers.Leitura anteriorContainers;
    private volatile boolean tabelasProntas;
    private volatile LocalDateTime ultimaGravacaoDiscos;

    public MonitorService(LeitorSistema leitor, LeitorContainers leitorContainers, MonitorRepository repository,
                          MonitorProperties props) {
        this.leitor = leitor;
        this.leitorContainers = leitorContainers;
        this.repository = repository;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        if (!props.habilitado()) {
            log.info("Monitoramento do servidor desligado (monitor.habilitado=false)");
            return;
        }
        try {
            anterior = leitor.contadores();
        } catch (RuntimeException e) {
            log.warn("Monitoramento: não foi possível ler o sistema: {}", e.getMessage());
        }
        if (leitorContainers.habilitado()) {
            try {
                anteriorContainers = leitorContainers.ler();
                log.info("Monitoramento: {} containers encontrados", anteriorContainers.containers().size());
            } catch (RuntimeException e) {
                log.warn("Monitoramento: não foi possível ler os containers: {}", e.getMessage());
            }
        }
        prepararTabelas();
    }

    @Scheduled(cron = "${monitor.coleta-cron:0 * * * * *}")
    public void coletar() {
        if (!props.habilitado()) return;
        LocalDateTime agora = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        coletarServidor(agora);
        coletarContainers(agora);
    }

    private void coletarServidor(LocalDateTime agora) {
        try {
            LeitorSistema.Contadores atual = leitor.contadores();
            LeitorSistema.Contadores antes = anterior;
            anterior = atual;
            if (antes == null) return; // primeira leitura: ainda não há intervalo para calcular as médias

            LeitorSistema.Instantaneo inst = leitor.instantaneo();
            if (!prepararTabelas()) return;
            repository.inserir(Amostra.de(agora, antes, atual, inst));
            if (ultimaGravacaoDiscos == null || !agora.isBefore(ultimaGravacaoDiscos.plusMinutes(MINUTOS_ENTRE_DISCOS))) {
                repository.inserirDiscos(agora, inst.discos());
                ultimaGravacaoDiscos = agora;
            }
        } catch (RuntimeException e) {
            log.warn("Monitoramento: falha na coleta das {}: {}", agora.toLocalTime(), e.getMessage());
        }
    }

    private void coletarContainers(LocalDateTime agora) {
        if (!leitorContainers.habilitado()) return;
        try {
            LeitorContainers.Leitura atual = leitorContainers.ler();
            LeitorContainers.Leitura antes = anteriorContainers;
            anteriorContainers = atual;
            if (antes == null || !prepararTabelas()) return;
            repository.inserirContainers(AmostraContainer.de(agora, antes, atual));
        } catch (RuntimeException e) {
            log.warn("Monitoramento: falha na coleta dos containers das {}: {}", agora.toLocalTime(), e.getMessage());
        }
    }

    @Scheduled(cron = "0 15 3 * * *")
    public void limparAntigas() {
        if (!props.habilitado() || !prepararTabelas()) return;
        try {
            int apagadas = repository.apagarAntesDe(LocalDate.now().minusDays(props.retencaoDias()).atStartOfDay());
            if (apagadas > 0) log.info("Monitoramento: {} amostras com mais de {} dias apagadas", apagadas, props.retencaoDias());
            int containers = repository.apagarContainersAntesDe(
                    LocalDate.now().minusDays(props.retencaoContainersDias()).atStartOfDay());
            if (containers > 0) {
                log.info("Monitoramento: {} amostras de containers com mais de {} dias apagadas", containers,
                        props.retencaoContainersDias());
            }
        } catch (DataAccessException e) {
            log.warn("Monitoramento: falha ao apagar amostras antigas: {}", e.getMessage());
        }
    }

    /**
     * Leitura ao vivo: CPU, rede e disco medidos num intervalo de meio segundo. Se o Docker não responder,
     * a leitura do servidor sai mesmo assim, só sem os containers.
     */
    public ServidorAgoraResponse agora() {
        try {
            LeitorSistema.Contadores antes = leitor.contadores();
            LeitorContainers.Leitura containersAntes = lerContainersSemFalhar();
            Thread.sleep(MEIO_SEGUNDO);
            LeitorSistema.Contadores atual = leitor.contadores();
            LeitorContainers.Leitura containersDepois = containersAntes == null ? null : lerContainersSemFalhar();
            LeitorSistema.Instantaneo inst = leitor.instantaneo();
            LeitorSistema.Identificacao id = leitor.identificacao();
            List<ServidorContainerResponse> containers = containersDepois == null ? List.of()
                    : containersAgora(containersAntes, containersDepois);
            return montarAgora(id, antes, atual, inst, containers);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MonitorIndisponivelException("Leitura do servidor interrompida", e);
        } catch (RuntimeException e) {
            throw new MonitorIndisponivelException("Não foi possível ler as métricas do servidor", e);
        }
    }

    public ServidorDiaResponse dia(LocalDate data) {
        LocalDateTime de = data.atStartOfDay();
        LocalDateTime ate = data.plusDays(1).atStartOfDay();
        return historico(() -> new ServidorDiaResponse(data, repository.resumo(de, ate), repository.serie(de, ate),
                repository.discos(de, ate), repository.containers(de, ate), repository.serieContainers(de, ate)));
    }

    public List<ServidorResumoDiaResponse> dias(LocalDate de, LocalDate ate) {
        Assert.isTrue(!de.isAfter(ate), "A data inicial deve ser anterior ou igual à final");
        Assert.isTrue(ChronoUnit.DAYS.between(de, ate) < MAX_DIAS_CONSULTA,
                "Consulte no máximo " + MAX_DIAS_CONSULTA + " dias por vez");
        return historico(() -> repository.porDia(de, ate));
    }

    private LeitorContainers.Leitura lerContainersSemFalhar() {
        if (!leitorContainers.habilitado()) return null;
        try {
            return leitorContainers.ler();
        } catch (RuntimeException e) {
            log.warn("Monitoramento: não foi possível ler os containers: {}", e.getMessage());
            return null;
        }
    }

    /** Rodando primeiro, depois os que mais usam CPU. */
    static List<ServidorContainerResponse> containersAgora(LeitorContainers.Leitura antes, LeitorContainers.Leitura depois) {
        return AmostraContainer.de(LocalDateTime.now(), antes, depois).stream()
                .map(a -> new ServidorContainerResponse(a.nome(), a.imagem(), a.estado(), a.status(), a.saude(),
                        arredondar(a.cpuPct()), a.memUsada(), a.memLimite(), arredondar(a.memPct()),
                        arredondar(a.redeRxBps()), arredondar(a.redeTxBps()),
                        arredondar(a.discoLeituraBps()), arredondar(a.discoEscritaBps()), a.processos()))
                .sorted(Comparator.comparing((ServidorContainerResponse c) -> !"running".equals(c.estado()))
                        .thenComparing(c -> c.cpuPct() == null ? -1 : c.cpuPct(), Comparator.reverseOrder())
                        .thenComparing(ServidorContainerResponse::nome))
                .toList();
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

    /** Cria as tabelas na primeira vez que o MariaDB responde. */
    private boolean prepararTabelas() {
        if (tabelasProntas) return true;
        try {
            repository.criarTabelas();
            tabelasProntas = true;
            log.info("Monitoramento: tabelas do histórico prontas no MariaDB");
        } catch (DataAccessException e) {
            log.warn("Monitoramento: MariaDB indisponível: {}", e.getMostSpecificCause().getMessage());
        }
        return tabelasProntas;
    }

    static ServidorAgoraResponse montarAgora(LeitorSistema.Identificacao id, LeitorSistema.Contadores antes,
                                              LeitorSistema.Contadores atual, LeitorSistema.Instantaneo inst,
                                              List<ServidorContainerResponse> containers) {
        double seg = atual.segundosDesde(antes);

        List<ServidorAgoraResponse.Nucleo> nucleos = new ArrayList<>();
        for (int i = 0; i < atual.nucleos().size() && i < antes.nucleos().size(); i++) {
            Long mhz = i < inst.frequenciasMhz().size() ? inst.frequenciasMhz().get(i) : null;
            nucleos.add(new ServidorAgoraResponse.Nucleo(i, arredondar(atual.nucleos().get(i).usoPct(antes.nucleos().get(i))), mhz));
        }

        List<ServidorAgoraResponse.Rede> rede = new ArrayList<>();
        atual.rede().forEach((nome, t) -> {
            LeitorSistema.Trafego a = antes.rede().get(nome);
            if (a == null) return;
            rede.add(new ServidorAgoraResponse.Rede(nome,
                    Math.round(Math.max(t.recebidos() - a.recebidos(), 0) / seg),
                    Math.round(Math.max(t.enviados() - a.enviados(), 0) / seg)));
        });

        return new ServidorAgoraResponse(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), id.host(), id.modelo(), id.kernel(), inst.uptimeSegundos(),
                new ServidorAgoraResponse.Cpu(arredondar(atual.cpu().usoPct(antes.cpu())),
                        arredondar(atual.cpu().iowaitPct(antes.cpu())), nucleos),
                new ServidorAgoraResponse.Carga(inst.load1(), inst.load5(), inst.load15(), inst.processos()),
                new ServidorAgoraResponse.Memoria(inst.memTotal(), inst.memUsada(), arredondar(inst.memPct()),
                        inst.swapTotal(), inst.swapUsada(), arredondar(inst.swapPct())),
                inst.temperaturas().stream().map(t -> new ServidorAgoraResponse.Temperatura(t.sensor(), t.celsius())).toList(),
                inst.temperaturaMax(),
                inst.discos().stream().map(d -> new ServidorDiscoResponse(d.nome(), d.caminho(), d.total(), d.usado(),
                        arredondar(d.pct()))).toList(),
                rede,
                new ServidorAgoraResponse.DiscoIo(
                        Math.round(Math.max(atual.discoLidos() - antes.discoLidos(), 0) / seg),
                        Math.round(Math.max(atual.discoEscritos() - antes.discoEscritos(), 0) / seg)),
                containers);
    }

    private static double arredondar(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private static Double arredondar(Double v) {
        return v == null ? null : arredondar(v.doubleValue());
    }
}
