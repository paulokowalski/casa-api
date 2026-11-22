package com.kowalski.casaapi.api.v1.input;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Rota monitorada pelo robô de passagens. Informe `datas` (datas fixas) OU `janela` (período flexível).")
public record RotaPassagemInput(
        @Schema(description = "Nome único da rota", example = "Fortaleza - São Paulo (set/2027)") String nome,
        @Schema(description = "false = pausada (o robô não busca, o histórico fica)", example = "true") Boolean ativa,
        @Schema(description = "Aeroportos de origem (código IATA)", example = "[\"FOR\"]") List<String> origem,
        @Schema(description = "Aeroportos de destino (código IATA)", example = "[\"GRU\", \"CGH\"]") List<String> destino,
        @Schema(description = "Datas fixas; volta vazia = só ida") List<Data> datas,
        @Schema(description = "Período flexível (no lugar das datas fixas)") Janela janela,
        @Schema(description = "Alerta quando o preço ficar igual ou abaixo disto (R$)", example = "1000") Integer precoAlvo,
        @Schema(description = "Máximo de escalas (vazio = qualquer)", example = "1") Integer maxEscalas,
        @Schema(description = "Alerta quando o preço ficar X% abaixo da média dos últimos 30 dias", example = "25") Integer quedaPct,
        @Schema(description = "Adultos", example = "1") Integer adultos,
        @Schema(description = "Crianças de 2 a 11 anos", example = "0") Integer criancas,
        @Schema(description = "Bebês (até 2 anos) com assento próprio", example = "0") Integer bebesAssento,
        @Schema(description = "Bebês (até 2 anos) no colo; no máximo um por adulto", example = "0") Integer bebesColo) {

    public record Data(@JsonFormat(pattern = "yyyy-MM-dd") LocalDate ida,
                       @JsonFormat(pattern = "yyyy-MM-dd") LocalDate volta) { }

    @Schema(description = "Testa uma ida a cada `passoDias` entre `inicio` e `fim`; com `duracao` [mín, máx] em dias, é ida e volta")
    public record Janela(@JsonFormat(pattern = "yyyy-MM-dd") LocalDate inicio,
                         @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fim,
                         Integer passoDias,
                         List<Integer> duracao,
                         @Schema(description = "Só estes dias da semana (0 = segunda ... 6 = domingo)") List<Integer> diasSemana) { }
}
