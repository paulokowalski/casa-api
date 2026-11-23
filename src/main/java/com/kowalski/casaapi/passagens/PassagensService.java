package com.kowalski.casaapi.passagens;

import com.kowalski.casaapi.comum.MonitorIndisponivelException;
import com.kowalski.casaapi.passagens.PassagensRepository.Robo;
import com.kowalski.casaapi.passagens.PassagensResponse.Alerta;
import com.kowalski.casaapi.passagens.PassagensResponse.Serie;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Aba Passagens: lê o schema {@code passagens} do Postgres, preenchido pelo robô de passagens
 * (/opt/projetos/robo-passagens) depois de cada rodada. A API só lê.
 */
@Service
@RequiredArgsConstructor
public class PassagensService {

    /** Acima disto o gráfico mostra o menor preço de cada dia, em vez de um ponto por busca. */
    static final int DIAS_POR_BUSCA = 31;
    /** Folga além de duas rodadas do robô antes de considerar que ele parou. */
    static final Duration FOLGA = Duration.ofMinutes(30);

    private final PassagensRepository repository;
    private final Clock relogio;

    /** @param dias período dos pontos do gráfico; 0 = todo o histórico */
    public PassagensResponse listar(int dias) {
        try {
            return montar(Math.max(0, dias));
        } catch (DataAccessException e) {
            throw new MonitorIndisponivelException("Histórico de passagens indisponível (Postgres)", e);
        }
    }

    private PassagensResponse montar(int dias) {
        Robo robo = repository.robo()
                .orElseThrow(() -> new MonitorIndisponivelException("O robô de passagens ainda não enviou dados", null));
        OffsetDateTime sincronizado = robo.sincronizadoEm();
        double intervalo = robo.intervaloHoras();

        List<Map<String, Object>> rotas = repository.rotas();
        boolean porBusca = dias > 0 && dias <= DIAS_POR_BUSCA;
        List<Serie> series = repository.series(repository.pontos(dias, porBusca));
        List<Alerta> alertas = repository.alertas();

        return new PassagensResponse(sincronizado, intervalo, robo.moeda(),
                desatualizado(sincronizado, intervalo, Instant.now(relogio)), dias, porBusca ? "busca" : "dia",
                rotas, series, alertas);
    }

    static boolean desatualizado(OffsetDateTime sincronizado, double intervaloHoras, Instant agora) {
        if (sincronizado == null) return true;
        double horas = intervaloHoras > 0 ? intervaloHoras : 6;
        Duration validade = Duration.ofMinutes(Math.round(horas * 2 * 60)).plus(FOLGA);
        return sincronizado.toInstant().isBefore(agora.minus(validade));
    }
}
