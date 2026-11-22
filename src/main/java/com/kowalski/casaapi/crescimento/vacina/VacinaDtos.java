package com.kowalski.casaapi.crescimento.vacina;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public final class VacinaDtos {

    private VacinaDtos() {
    }

    @Schema(description = "Dose aplicada")
    public record VacinaInput(@Schema(description = "Padrão: hoje") LocalDate aplicadaEm, String lote,
                              @Schema(description = "Unidade de saúde ou clínica") String local) {
    }

    @Schema(description = "Vacina fora do calendário do SUS (particular, campanha...)")
    public record VacinaExtraInput(@Schema(example = "Meningocócica B") String nome, LocalDate aplicadaEm, String lote, String local) {
    }

    public enum SituacaoVacina {
        @Schema(description = "aplicada") APLICADA,
        @Schema(description = "na idade de tomar (até 30 dias depois da data)") TOMAR_AGORA,
        @Schema(description = "passou mais de 30 dias da data") ATRASADA,
        @Schema(description = "nos próximos 30 dias") PROXIMA,
        @Schema(description = "mais adiante") FUTURA,
        @Schema(description = "passou da idade máxima (rotavírus); converse com a unidade de saúde") PRAZO_ENCERRADO
    }

    public record DoseResponse(String id, String vacina, String dose, int idadeMeses,
                               @Schema(description = "Data recomendada (pela idade real)") LocalDate dataPrevista,
                               @Schema(description = "Última data para aplicar, quando há limite") LocalDate dataLimite,
                               SituacaoVacina situacao, LocalDate aplicadaEm, String lote, String local) {
    }

    @Schema(description = "Doses de uma idade (ex.: 2 meses)")
    public record GrupoResponse(int idadeMeses, String rotulo, LocalDate data, int aplicadas, int total, List<DoseResponse> doses) {
    }

    public record ExtraResponse(String id, String nome, LocalDate aplicadaEm, String lote, String local) {
    }

    @Schema(description = "Carteira de vacinação")
    public record VacinasResponse(String versaoCalendario, String aviso, int aplicadas, int total, int pendentes,
                                  @Schema(description = "Data da próxima dose ainda não aplicada (pode estar no passado, se atrasada)") LocalDate proximaData,
                                  List<GrupoResponse> grupos, List<ExtraResponse> extras) {
    }
}
