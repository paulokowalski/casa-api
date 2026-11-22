package com.kowalski.casaapi.crescimento.marco;

import com.kowalski.casaapi.crescimento.marco.CatalogoMarcos.Area;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public final class MarcoDtos {

    private MarcoDtos() {
    }

    @Schema(description = "Marco atingido")
    public record MarcoInput(@Schema(description = "Padrão: hoje") LocalDate atingidoEm,
                             @Schema(description = "Anotação opcional") String nota) {
    }

    public enum SituacaoFaixa { PASSADA, ATUAL, PROXIMA, FUTURA }

    public enum SituacaoMarco {
        @Schema(description = "já aconteceu") ATINGIDO,
        @Schema(description = "esperado na fase atual") ESPERADO,
        @Schema(description = "de uma fase que já passou e sem registro: se não aconteceu, converse com o pediatra") ATENCAO,
        @Schema(description = "de uma fase futura") FUTURO
    }

    @Schema(description = "Marco do desenvolvimento")
    public record MarcoResponse(String id, Area area, String texto, SituacaoMarco situacao, LocalDate atingidoEm,
                                @Schema(description = "Idade quando atingiu, por extenso") String idadeAoAtingir, String nota) {
    }

    @Schema(description = "Faixa de idade com seus marcos")
    public record FaixaResponse(String id, String nome, int inicioMeses, int fimMeses, SituacaoFaixa situacao,
                                @Schema(description = "Brincadeiras para estimular nesta fase") List<String> brincadeiras,
                                int atingidos, int total, List<MarcoResponse> marcos) {
    }

    @Schema(description = "Marcos do desenvolvimento da criança")
    public record MarcosResponse(String versaoCatalogo, String fonte,
                                 @Schema(description = "Idade usada (corrigida para prematuros até 2 anos), em dias") int idadeDias,
                                 boolean idadeCorrigida, String faixaAtualId,
                                 @Schema(description = "Marcos de fases passadas sem registro") int marcosAtencao,
                                 String aviso, List<FaixaResponse> faixas) {
    }
}
