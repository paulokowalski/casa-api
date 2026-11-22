package com.kowalski.casaapi.business.monitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InternetTest {

    private static final String INTERNET = """
            {"versao":1,"lidoEm":"2026-11-21T12:24:36-03:00","online":true,
             "ping":[{"id":"roteador","alvo":"192.168.68.1","perdaPct":0.0,"mediaMs":0.8,"maxMs":1.0,"jitterMs":0.1},
                     {"id":"cloudflare","alvo":"1.1.1.1","perdaPct":20.0,"mediaMs":3.7,"maxMs":5.3,"jitterMs":0.8},
                     {"id":"google","alvo":"8.8.8.8","perdaPct":10.0,"mediaMs":56.5,"maxMs":57.4,"jitterMs":0.5}],
             "dns":[{"id":"pihole","ok":true,"tempoMs":3},{"id":"cloudflare","ok":false,"tempoMs":null}],
             "publico":{"ip":"200.223.118.230","provedor":"V tal"},
             "pihole":{"ok":true,"bloqueadasPct":22.6,"consultas24h":66662}}
            """;

    private static final String VELOCIDADE = """
            {"versao":1,"ultimo":{"testadoEm":"2026-11-21T12:25:44-03:00","ok":true,"downloadMbps":718.3},
             "historico":[{"testadoEm":"2026-11-21T12:25:44-03:00","ok":true,"downloadMbps":718.3,"uploadMbps":363.1,
                           "latenciaMs":35.1,"bytes":250663296},
                          {"testadoEm":"2026-11-21T08:07:00-03:00","ok":false,"erro":"timeout"},
                          {"ok":true}]}
            """;

    @Test
    void amostraPegaCadaAlvoPeloId(@TempDir Path dir) throws Exception {
        LeitorInternet leitor = leitor(dir);
        AmostraInternet a = AmostraInternet.de(LocalDateTime.of(2026, 11, 21, 12, 24), leitor.internet());

        assertThat(a.online()).isTrue();
        assertThat(a.roteador().mediaMs()).isEqualTo(0.8);
        assertThat(a.modem()).isEqualTo(AmostraInternet.Ping.VAZIO);
        assertThat(a.cloudflare().jitterMs()).isEqualTo(0.8);
        assertThat(a.perdaInternet()).isEqualTo(10.0);
        assertThat(a.pihole().tempoMs()).isEqualTo(3.0);
        assertThat(a.dnsCloudflare().ok()).isFalse();
        assertThat(a.dnsGoogle()).isEqualTo(AmostraInternet.Dns.VAZIO);
        assertThat(a.ipPublico()).isEqualTo("200.223.118.230");
        assertThat(a.piholeConsultas24h()).isEqualTo(66662L);
    }

    @Test
    void testesIgnoramItensSemData(@TempDir Path dir) throws Exception {
        var testes = InternetService.testes(leitor(dir).velocidade());

        assertThat(testes).hasSize(2);
        assertThat(testes.get(0).downloadMbps()).isEqualTo(718.3);
        assertThat(testes.get(0).bytes()).isEqualTo(250663296L);
        assertThat(testes.get(1).ok()).isFalse();
        assertThat(testes.get(1).erro()).isEqualTo("timeout");
    }

    @Test
    void telaJuntaUltimoTesteEMarcaDesatualizado(@TempDir Path dir) throws Exception {
        Map<String, Object> tela = leitor(dir).lerParaTela();

        assertThat(tela).containsEntry("desatualizado", true).containsKey("velocidade");
        assertThat(LeitorInternet.desatualizado(Map.of("lidoEm", "2026-11-21T12:24:36-03:00"),
                Instant.parse("2026-11-21T15:26:00Z"))).isFalse();
        assertThat(new LeitorInternet(dir.resolve("nao-existe.json"), null).lerParaTela()).isNull();
    }

    private static LeitorInternet leitor(Path dir) throws Exception {
        Path i = Files.writeString(dir.resolve("internet.json"), INTERNET);
        Path v = Files.writeString(dir.resolve("velocidade.json"), VELOCIDADE);
        return new LeitorInternet(i, v);
    }
}
