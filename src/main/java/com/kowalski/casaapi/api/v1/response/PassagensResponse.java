package com.kowalski.casaapi.api.v1.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Rotas do robô de passagens e o histórico de preços (schema passagens do Postgres)")
public record PassagensResponse(
        @Schema(description = "Último envio do robô ao Postgres") OffsetDateTime geradoEm,
        @Schema(description = "De quantas em quantas horas o robô busca", example = "1") double intervaloHoras,
        @Schema(example = "BRL") String moeda,
        @Schema(description = "Robô sem enviar há mais de duas rodadas") boolean desatualizado,
        @Schema(description = "Dias do período pedido (0 = tudo)", example = "30") int dias,
        @Schema(description = "busca (um ponto por busca) ou dia (menor preço de cada dia)", example = "busca") String agrupamento,
        @Schema(description = "Rotas como estão no config.yaml do robô, na mesma ordem") List<Map<String, Object>> rotas,
        @Schema(description = "Uma série por trecho e datas (só viagens de hoje em diante)") List<Serie> historico,
        @Schema(description = "Alertas de passagem barata enviados no Telegram, mais recentes primeiro") List<Alerta> alertas) {

    @Schema(description = "Preços de um trecho e datas ao longo do tempo. Mínimo, máximo, média e buscas são de todo o histórico.")
    public record Serie(String rota, String origem, String destino, LocalDate ida, LocalDate volta, String url,
                        @Schema(description = "Pontos do período pedido") List<Ponto> pontos,
                        Ponto ultimo, Ponto minimo, int maximo, int media, long buscas) { }

    public record Ponto(OffsetDateTime em, int preco, String cias, Integer escalas) { }

    public record Alerta(String rota, String origem, String destino, LocalDate ida, LocalDate volta, int preco,
                         OffsetDateTime em) { }
}
