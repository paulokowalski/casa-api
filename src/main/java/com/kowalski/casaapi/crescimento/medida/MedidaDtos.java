package com.kowalski.casaapi.crescimento.medida;

import com.kowalski.casaapi.crescimento.oms.Indicador;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MedidaDtos {

    private MedidaDtos() {
    }

    @Schema(description = "Uma pesagem/medição. Informe pelo menos uma medida.")
    public record MedicaoInput(@Schema(example = "2026-11-16") LocalDate data,
                               @Schema(example = "7.35") Double pesoKg,
                               @Schema(example = "66.5") Double comprimentoCm,
                               @Schema(example = "43.0") Double perimetroCm,
                               @Schema(example = "Pesagem na UBS") String observacao) {
    }

    public enum Faixa {
        @Schema(description = "abaixo do P3") ABAIXO,
        @Schema(description = "entre P3 e P15") LIMITE_INFERIOR,
        @Schema(description = "entre P15 e P85") ESPERADA,
        @Schema(description = "entre P85 e P97") LIMITE_SUPERIOR,
        @Schema(description = "acima do P97") ACIMA
    }

    @Schema(description = "Valor da medida comparado à OMS; z, percentil e faixa ficam null fora das tabelas (antes do termo ou após 5 anos)")
    public record MedidaAvaliada(Double valor, Double z, Double percentil, Faixa faixa) {
    }

    @Schema(description = "Medição com percentis")
    public record MedicaoResponse(UUID id, LocalDate data, int idadeDias, String idade,
                                  MedidaAvaliada peso, MedidaAvaliada comprimento, MedidaAvaliada perimetro,
                                  @Schema(description = "Índice de massa corporal (kg/m²), quando há peso e comprimento") Double imc,
                                  String observacao, Long criadoPor) {
    }

    @Schema(description = "Ganho de peso entre as duas últimas pesagens (com pelo menos ~3 semanas entre elas, quando houver)")
    public record GanhoPeso(int gramas, int dias, double gramasPorDia,
                            @Schema(description = "Faixa de referência de ganho para a idade, em g/dia") int esperadoMinimo,
                            int esperadoMaximo,
                            @Schema(description = "ABAIXO, ESPERADO ou ACIMA") String situacao) {
    }

    @Schema(description = "Última medida de um indicador")
    public record UltimaMedida(LocalDate data, Double valor, Double z, Double percentil, Faixa faixa) {
    }

    public enum Nivel { ATENCAO, INFO }

    @Schema(description = "Algo que vale a atenção dos pais")
    public record Alerta(@Schema(example = "CRUZOU_LINHAS") String codigo, Nivel nivel, Indicador indicador,
                         String titulo, String texto) {
    }

    @Schema(description = "Resumo do crescimento para a tela inicial")
    public record ResumoResponse(UUID criancaId, LocalDate hoje, int idadeDias, Integer idadeCorrigidaDias, String idade,
                                 Map<Indicador, UltimaMedida> ultimas, GanhoPeso ganhoPeso,
                                 @Schema(description = "Dias desde a última pesagem; null se nunca pesou") Integer diasDesdeUltimaPesagem,
                                 @Schema(description = "Próxima consulta marcada no diário (retorno), hoje ou depois") LocalDate proximaConsulta,
                                 List<Alerta> alertas) {
    }

    @Schema(description = "Linhas de percentil da OMS numa idade (dias na idade da curva: corrigida para prematuros)")
    public record PontoReferencia(int dias, double p3, double p15, double p50, double p85, double p97) {
    }

    @Schema(description = "Medida da criança no gráfico")
    public record PontoCrianca(LocalDate data, int dias, double valor, Double z, Double percentil,
                               @Schema(description = "Valor vem do cadastro (ao nascer), não de uma medição") boolean nascimento) {
    }

    @Schema(description = "Dados para o gráfico de um indicador")
    public record CurvaResponse(Indicador indicador, String nome, String unidade,
                                @Schema(description = "Os dias estão na idade corrigida (prematuro)") boolean idadeCorrigida,
                                List<PontoReferencia> referencia, List<PontoCrianca> pontos) {
    }
}
