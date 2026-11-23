package com.kowalski.casaapi.conta;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

import static com.kowalski.casaapi.comum.Competencias.parseOuNulo;
import static com.kowalski.casaapi.comum.Competencias.parse;

@Schema(description = "Dados para cadastrar ou editar uma conta")
public record ContaInput(
        @Schema(description = "Id da pessoa dona da conta", example = "1") Long pessoaId,
        @Schema(description = "Id da categoria (opcional)") UUID categoriaId,
        @Schema(description = "Descrição (até 120 caracteres)", example = "Internet") String descricao,
        @Schema(description = "RECORRENTE (todo mês, com ou sem fim), PARCELADA (número fixo de parcelas) ou UNICA (um mês só)", example = "RECORRENTE") TipoConta tipo,
        @Schema(description = "Valor mensal (maior que zero); em conta de valor variável, é a estimativa", example = "129.90") BigDecimal valor,
        @Schema(description = "Dia do vencimento (1 a 31; em meses mais curtos, vira o último dia)", example = "10") Integer diaVencimento,
        @Schema(description = "Primeiro mês da conta (aaaa-mm)", example = "2026-01") String competenciaInicio,
        @Schema(description = "Último mês (aaaa-mm), só para RECORRENTE; vazio = sem fim") String competenciaFim,
        @Schema(description = "PARCELADA: número da parcela no primeiro mês (padrão 1), para cadastrar parcelamentos já em andamento", example = "1") Integer parcelaInicial,
        @Schema(description = "PARCELADA: total de parcelas", example = "12") Integer totalParcelas,
        @Schema(description = "PARCELADA: juros ao mês em %, opcional (usado para calcular a economia ao antecipar parcelas)", example = "1.99") BigDecimal jurosMes,
        @Schema(description = "O valor muda todo mês (ex.: luz); o valor informado é só a estimativa", example = "false") Boolean valorVariavel,
        @Schema(description = "Pago em débito automático: lançamentos vencidos são baixados sozinhos", example = "false") Boolean debitoAutomatico) {

    public DadosDaConta paraDados() {
        return new DadosDaConta(descricao, tipo, valor, diaVencimento, parse(competenciaInicio, "competenciaInicio"),
                tipo == TipoConta.RECORRENTE ? parseOuNulo(competenciaFim, "competenciaFim") : null,
                parcelaInicial, totalParcelas, jurosMes, Boolean.TRUE.equals(valorVariavel), Boolean.TRUE.equals(debitoAutomatico));
    }
}
