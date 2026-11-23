package com.kowalski.casaapi.ventilacao;

import com.kowalski.casaapi.ventilacao.VentilacaoResponse.Leitura;
import com.kowalski.casaapi.ventilacao.VentilacaoResponse.Limite;
import com.kowalski.casaapi.comum.MonitorIndisponivelException;
import com.kowalski.casaapi.config.monitor.MonitorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Ventilação do servidor: o Home Assistant envia a cada minuto (e quando a tomada muda) o estado da tomada das
 * ventoinhas, as temperaturas que ele usa para decidir e os limites de ligar/desligar. A API guarda uma linha por
 * minuto em {@code amostra_ventilacao}; o tempo ligado é a contagem de minutos com a tomada ligada.
 */
@Slf4j
@Service
public class VentilacaoService {

    static final int MAX_DIAS_CONSULTA = 366;
    /** O HA envia a cada minuto: passou disso, a leitura está parada. */
    static final Duration DESATUALIZADO = Duration.ofMinutes(3);
    /** Minutos sem leitura que ainda contam como o mesmo período ligado. */
    static final int FOLGA_PERIODO_MIN = 2;
    static final Set<String> ESTADOS = Set.of("on", "off", "unavailable", "unknown");
    private static final String LIGADA = "on";

    private final VentilacaoRepository repository;
    private final MonitorProperties props;
    private final Clock relogio;
    private volatile boolean tabelaPronta;

    public VentilacaoService(VentilacaoRepository repository, MonitorProperties props, Clock relogio) {
        this.repository = repository;
        this.props = props;
        this.relogio = relogio;
    }

    public void registrar(Leitura recebida) {
        Assert.notNull(recebida, "Leitura obrigatória");
        Assert.isTrue(recebida.tomada() != null && ESTADOS.contains(recebida.tomada()),
                "tomada deve ser um de " + ESTADOS);
        Leitura l = new Leitura(recebida.tomada(), recebida.precisa(), recebida.automatica(),
                temperatura(recebida.soc()), temperatura(recebida.hds()), temperatura(recebida.nvme()),
                limite(recebida.limiteSoc()), limite(recebida.limiteHds()), limite(recebida.limiteNvme()));
        LocalDateTime agora = LocalDateTime.now(relogio);
        historico(() -> {
            repository.gravar(agora.truncatedTo(ChronoUnit.MINUTES), agora.truncatedTo(ChronoUnit.SECONDS), l);
            return null;
        });
    }

    public VentilacaoResponse.Agora agora() {
        return historico(() -> {
            VentilacaoRepository.Ultima u = repository.ultima().orElseThrow(() ->
                    new MonitorIndisponivelException("Nenhuma leitura da ventilação ainda (o Home Assistant envia a cada minuto)", null));
            LocalDateTime minuto = u.minuto();
            LocalDateTime recebido = u.recebidoEm();
            Leitura l = u.leitura();
            LocalDate hoje = LocalDate.now(relogio);
            List<VentilacaoResponse.ResumoDia> dia = repository.porDia(hoje.atStartOfDay(), hoje.plusDays(1).atStartOfDay());
            boolean desatualizado = Duration.between(recebido, LocalDateTime.now(relogio)).compareTo(DESATUALIZADO) > 0;
            return new VentilacaoResponse.Agora(recebido, desatualizado, l, repository.desde(l.tomada(), minuto),
                    dia.isEmpty() ? 0 : dia.get(0).minutosLigada(), dia.isEmpty() ? 0 : dia.get(0).minutosComDados());
        });
    }

    public VentilacaoResponse.Dia dia(LocalDate data) {
        LocalDateTime de = data.atStartOfDay();
        LocalDateTime ate = data.plusDays(1).atStartOfDay();
        return historico(() -> {
            List<VentilacaoResponse.ResumoDia> r = repository.porDia(de, ate);
            List<VentilacaoRepository.Minuto> minutos = repository.minutos(de, ate);
            return new VentilacaoResponse.Dia(
                    r.isEmpty() ? new VentilacaoResponse.ResumoDia(data, 0, 0, 0, null, null, null) : r.get(0),
                    periodos(minutos), pontos(minutos));
        });
    }

    public List<VentilacaoResponse.ResumoDia> dias(LocalDate de, LocalDate ate) {
        Assert.isTrue(!de.isAfter(ate), "A data inicial deve ser anterior ou igual à final");
        Assert.isTrue(ChronoUnit.DAYS.between(de, ate) < MAX_DIAS_CONSULTA,
                "Consulte no máximo " + MAX_DIAS_CONSULTA + " dias por vez");
        return historico(() -> repository.porDia(de.atStartOfDay(), ate.plusDays(1).atStartOfDay()));
    }

    @Scheduled(cron = "0 25 3 * * *")
    public void limparAntigas() {
        if (!props.habilitado() || !prepararTabela()) return;
        try {
            repository.apagarAntesDe(LocalDate.now(relogio).minusDays(props.retencaoDias()).atStartOfDay());
        } catch (DataAccessException e) {
            log.warn("Monitoramento: falha ao apagar leituras antigas da ventilação: {}", e.getMessage());
        }
    }

    /** Junta os minutos ligados seguidos (tolerando {@link #FOLGA_PERIODO_MIN} minutos sem leitura). */
    static List<VentilacaoResponse.Periodo> periodos(List<VentilacaoRepository.Minuto> minutos) {
        List<VentilacaoResponse.Periodo> r = new ArrayList<>();
        LocalDateTime inicio = null;
        LocalDateTime ultimo = null;
        long contados = 0;
        for (VentilacaoRepository.Minuto m : minutos) {
            boolean ligada = LIGADA.equals(m.leitura().tomada());
            boolean emendado = ultimo != null && !m.em().isAfter(ultimo.plusMinutes(FOLGA_PERIODO_MIN + 1L));
            if (ligada && inicio != null && emendado) {
                ultimo = m.em();
                contados++;
            } else {
                if (inicio != null) r.add(new VentilacaoResponse.Periodo(inicio, ultimo.plusMinutes(1), contados));
                inicio = ligada ? m.em() : null;
                ultimo = ligada ? m.em() : null;
                contados = ligada ? 1 : 0;
            }
        }
        if (inicio != null) r.add(new VentilacaoResponse.Periodo(inicio, ultimo.plusMinutes(1), contados));
        return r;
    }

    /** Um ponto por minuto gravado; HDs = 0 (todos em repouso) vira vazio para não derrubar a linha. */
    static List<VentilacaoResponse.Ponto> pontos(List<VentilacaoRepository.Minuto> minutos) {
        return minutos.stream().map(m -> {
            Leitura l = m.leitura();
            Integer ligada = switch (l.tomada()) {
                case "on" -> 1;
                case "off" -> 0;
                default -> null;
            };
            Double hds = l.hds() != null && l.hds() > 0 ? l.hds() : null;
            return new VentilacaoResponse.Ponto("%02d:%02d".formatted(m.em().getHour(), m.em().getMinute()),
                    ligada, l.soc(), hds, l.nvme());
        }).toList();
    }

    private static Double temperatura(Double v) {
        return v == null || !Double.isFinite(v) || v < -40 || v > 150 ? null : Math.round(v * 10) / 10.0;
    }

    private static Limite limite(Limite l) {
        return l == null ? null : new Limite(temperatura(l.liga()), temperatura(l.desliga()));
    }

    private <T> T historico(Supplier<T> consulta) {
        if (!prepararTabela()) {
            throw new MonitorIndisponivelException("O banco do monitoramento (MariaDB) está indisponível", null);
        }
        try {
            return consulta.get();
        } catch (DataAccessException e) {
            throw new MonitorIndisponivelException("O banco do monitoramento (MariaDB) está indisponível", e);
        }
    }

    private boolean prepararTabela() {
        if (tabelaPronta) return true;
        try {
            repository.criarTabela();
            tabelaPronta = true;
        } catch (DataAccessException e) {
            log.warn("Monitoramento: MariaDB indisponível (ventilação): {}", e.getMostSpecificCause().getMessage());
        }
        return tabelaPronta;
    }
}
