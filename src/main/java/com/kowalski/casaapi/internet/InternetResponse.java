package com.kowalski.casaapi.internet;

import com.kowalski.casaapi.servidor.ServidorEstatisticasResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Histórico da internet da casa (amostra_internet a cada minuto, teste_velocidade a cada 4 horas). */
public final class InternetResponse {

    private InternetResponse() {
    }

    @Schema(description = "Um teste de velocidade")
    public record TesteVelocidade(
            @Schema(description = "Quando rodou") LocalDateTime testadoEm,
            @Schema(description = "Terminou sem erro") boolean ok,
            @Schema(description = "Download (Mbit/s)", example = "700.5") Double downloadMbps,
            @Schema(description = "Upload (Mbit/s)", example = "350.2") Double uploadMbps,
            @Schema(description = "Latência HTTP sem carga (ms)", example = "35") Double latenciaMs,
            @Schema(description = "Variação da latência (ms)", example = "9") Double jitterMs,
            @Schema(description = "Latência durante o download (ms): muito acima da sem carga = bufferbloat") Double latenciaCargaMs,
            @Schema(description = "Bytes trafegados no teste") Long bytes,
            @Schema(description = "Erro, se houve") String erro) { }

    @Schema(description = "Resumo da internet num período")
    public record Resumo(
            @Schema(description = "Minutos com amostra", example = "1440") long amostras,
            @Schema(description = "Minutos sem internet", example = "0") long minutosOffline,
            @Schema(description = "Ping até a Cloudflare (ms)") ServidorEstatisticasResponse.Faixa ping,
            @Schema(description = "Jitter médio até a Cloudflare (ms)") Double jitterMedio,
            @Schema(description = "Perda de pacotes média até a internet (%)") Double perdaMedia,
            @Schema(description = "Minutos com alguma perda de pacotes até a internet") long minutosComPerda,
            @Schema(description = "Ping médio até o roteador (ms)") Double roteadorMedio,
            @Schema(description = "Ping médio até o modem (ms)") Double modemMedio,
            @Schema(description = "Ping médio até o Google (ms)") Double googleMedio,
            @Schema(description = "Tempo médio do DNS pelo Pi-hole (ms)") Double dnsPiholeMedio,
            @Schema(description = "Minutos com o Pi-hole sem responder") long minutosDnsFalhando,
            @Schema(description = "Tempo médio do DNS direto na Cloudflare (ms)") Double dnsCloudflareMedio,
            @Schema(description = "Quantos IPs públicos diferentes no período") long ipsPublicos,
            @Schema(description = "Testes de velocidade no período") long testes,
            @Schema(description = "Download (Mbit/s) dos testes") ServidorEstatisticasResponse.Faixa download,
            @Schema(description = "Upload (Mbit/s) dos testes") ServidorEstatisticasResponse.Faixa upload) { }

    @Schema(description = "Médias de um bloco de 5 minutos")
    public record Ponto(
            @Schema(description = "Início do bloco (HH:mm)", example = "14:35") String horario,
            Double roteadorMs, Double modemMs, Double cloudflareMs,
            @Schema(description = "Pior ping até a Cloudflare no bloco") Double cloudflareMaxMs,
            Double googleMs, Double jitterMs,
            @Schema(description = "Perda de pacotes média até a internet (%)") Double perdaPct,
            @Schema(description = "% do bloco sem internet") double offlinePct,
            Double dnsPiholeMs, Double dnsCloudflareMs, Double dnsGoogleMs) { }

    @Schema(description = "Internet num dia")
    public record Dia(LocalDate data, Resumo resumo, List<Ponto> serie, List<TesteVelocidade> testes) { }

    @Schema(description = "Resumo de um dia")
    public record ResumoDia(LocalDate data, Resumo resumo) { }
}
