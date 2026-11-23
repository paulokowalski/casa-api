package com.kowalski.casaapi.business.monitor;

import com.kowalski.casaapi.servidor.ServidorRepository;
import com.kowalski.casaapi.servidor.EventosHost;
import com.kowalski.casaapi.api.v1.response.InternetResponse.TesteVelocidade;
import com.kowalski.casaapi.config.monitor.MonitorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/** Eventos da internet: troca de IP, períodos de perda de pacotes e testes de velocidade ruins. */
class InternetServiceEventosTest {

    static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 21, 12, 0);

    final LeitorInternet leitor = mock(LeitorInternet.class);
    final InternetRepository repository = mock(InternetRepository.class);
    final ServidorRepository monitorRepository = mock(ServidorRepository.class);
    final MonitorProperties props = mock(MonitorProperties.class);
    final InternetService service = new InternetService(leitor, repository, monitorRepository, props);
    final List<EventosHost.Evento> eventos = new ArrayList<>();

    @BeforeEach
    void ligado() {
        when(props.habilitado()).thenReturn(true);
        when(leitor.habilitado()).thenReturn(true);
        when(repository.ultimoIp()).thenReturn("1.1.1.1");
    }

    static AmostraInternet amostra(String ip, Boolean online, double perda) {
        var ping = new AmostraInternet.Ping(10.0, 12.0, 1.0, perda);
        var dns = new AmostraInternet.Dns(true, 3.0);
        return new AmostraInternet(T0, online, ping, ping, ping, ping, dns, dns, dns, ip, null, null);
    }

    /** Uma amostra por minuto a partir de T0, cada uma com a perda dada. */
    void minutos(double... perdas) {
        for (int i = 0; i < perdas.length; i++) service.eventosAmostra(T0.plusMinutes(i), amostra("1.1.1.1", true, perdas[i]), eventos);
    }

    @Test
    void ipPublicoQueMudaViraEvento() {
        service.eventosAmostra(T0, amostra("1.1.1.1", true, 0), eventos);
        assertThat(eventos).isEmpty(); // primeiro valor só é guardado
        service.eventosAmostra(T0.plusMinutes(1), amostra("2.2.2.2", true, 0), eventos);
        assertThat(eventos).singleElement().satisfies(e -> {
            assertThat(e.mensagem()).isEqualTo("IP público mudou de 1.1.1.1 para 2.2.2.2");
            assertThat(e.nivel()).isEqualTo(EventosHost.INFO);
        });
    }

    @Test
    void perdaPorTresMinutosAbreOPeriodoDesdeOPrimeiroMinuto() {
        minutos(20, 30);
        assertThat(eventos).isEmpty();
        service.eventosAmostra(T0.plusMinutes(2), amostra("1.1.1.1", true, 15), eventos);
        assertThat(eventos).singleElement().satisfies(e -> {
            assertThat(e.quando()).isEqualTo(T0);
            assertThat(e.nivel()).isEqualTo(EventosHost.ATENCAO);
            assertThat(e.chave()).isEqualTo("perda|" + T0);
        });
    }

    @Test
    void recuperacaoCurtaNaoEncerraEFimTemDuracaoEPico() {
        // 3 min com perda (abre), 2 sem (não encerra), 2 com perda (pico 60), 3 sem (encerra)
        minutos(20, 30, 15, 0, 0, 60, 40, 0, 0, 0);
        assertThat(eventos).hasSize(2);
        var fim = eventos.get(1);
        assertThat(fim.nivel()).isEqualTo(EventosHost.SUCESSO);
        assertThat(fim.quando()).isEqualTo(T0.plusMinutes(9));
        assertThat(fim.mensagem()).isEqualTo("Perda de pacotes acabou (durou ~7 min, pico de 60%)");
    }

    @Test
    void internetForaDoArNaoContaComoPerda() {
        for (int i = 0; i < 5; i++) service.eventosAmostra(T0.plusMinutes(i), amostra("1.1.1.1", false, 100), eventos);
        assertThat(eventos).isEmpty();
    }

    static TesteVelocidade teste(LocalDateTime quando, boolean ok, Double download, String erro) {
        return new TesteVelocidade(quando, ok, download, 100.0, 20.0, 1.0, 30.0, 250_000_000L, erro);
    }

    @Test
    void testeQueFalhaOuSaiAbaixoDaMetadeDaMedianaViraEvento() {
        service.eventosTeste(teste(T0, false, null, "timeout"), eventos);
        assertThat(eventos).singleElement().extracting(EventosHost.Evento::mensagem)
                .isEqualTo("Teste de velocidade falhou: timeout");

        eventos.clear();
        when(repository.downloadsAntes(any(), anyInt())).thenReturn(List.of(700.0, 650.0, 720.0, 680.0));
        service.eventosTeste(teste(T0, true, 400.0, null), eventos); // mediana 700: 400 não é lento
        assertThat(eventos).isEmpty();
        service.eventosTeste(teste(T0, true, 300.0, null), eventos);
        assertThat(eventos).singleElement().extracting(EventosHost.Evento::mensagem)
                .isEqualTo("Internet lenta: download de 300 Mbit/s (o normal é ~700)");
    }

    @Test
    void poucosTestesAnterioresNaoGeramEventoDeLentidao() {
        when(repository.downloadsAntes(any(), anyInt())).thenReturn(List.of(700.0, 650.0));
        service.eventosTeste(teste(T0, true, 10.0, null), eventos);
        assertThat(eventos).isEmpty();
    }

    @Test
    void soGravaOsTestesQuandoChegaUmTesteNovo() {
        Map<String, Object> v = Map.of("historico", List.of(Map.of("testadoEm", "2026-11-21T08:00:00-03:00", "ok", true)));
        when(leitor.velocidade()).thenReturn(v);
        when(repository.inserirTestes(anyList())).thenReturn(List.of());
        Instant agora = Instant.parse("2026-11-21T15:00:00Z");

        service.coletar(T0, agora);
        service.coletar(T0.plusMinutes(1), agora);
        verify(repository, times(1)).inserirTestes(anyList());

        when(leitor.velocidade()).thenReturn(Map.of("historico", List.of(
                Map.of("testadoEm", "2026-11-21T12:00:00-03:00", "ok", true),
                Map.of("testadoEm", "2026-11-21T08:00:00-03:00", "ok", true))));
        service.coletar(T0.plusMinutes(2), agora);
        verify(repository, times(2)).inserirTestes(anyList());
    }

    @Test
    void falhaAoGravarTentaDeNovoNoMinutoSeguinte() {
        when(leitor.velocidade()).thenReturn(
                Map.of("historico", List.of(Map.of("testadoEm", "2026-11-21T08:00:00-03:00", "ok", true))));
        when(repository.inserirTestes(anyList())).thenThrow(new IllegalStateException("banco fora")).thenReturn(List.of());
        Instant agora = Instant.parse("2026-11-21T15:00:00Z");

        service.coletar(T0, agora);
        service.coletar(T0.plusMinutes(1), agora);
        service.coletar(T0.plusMinutes(2), agora);
        verify(repository, times(2)).inserirTestes(anyList());
    }
}
