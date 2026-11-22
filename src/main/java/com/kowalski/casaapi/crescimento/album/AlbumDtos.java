package com.kowalski.casaapi.crescimento.album;

import com.kowalski.casaapi.crescimento.album.CatalogoAlbum.TipoLugar;
import com.kowalski.casaapi.crescimento.foto.FotoDtos.FotoResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AlbumDtos {

    private AlbumDtos() {
    }

    @Schema(description = "Fotos de um lugar do álbum. Envie as fotos antes (POST .../fotos) e passe os ids.")
    public record AlbumInput(@Schema(description = "Padrão: a data do mêsversário (ou hoje)") LocalDate data,
                             @Schema(description = "Legenda") String texto,
                             @Schema(description = "Nome do momento (só em especiais)", example = "Primeira praia") String albumTitulo,
                             @Schema(description = "Peso no dia (opcional)") Double pesoKg,
                             @Schema(description = "Fotos, na ordem; a última é a capa") List<UUID> fotoIds) {
    }

    public enum Situacao {
        @Schema(description = "tem foto") FEITO,
        @Schema(description = "já passou (ou é uma primeira vez) e não tem foto") PENDENTE,
        @Schema(description = "o mêsversário é hoje (±3 dias) e ainda não tem foto") HOJE,
        @Schema(description = "ainda não chegou") FUTURO
    }

    @Schema(description = "Um lugar do álbum")
    public record LugarResponse(String chave, TipoLugar tipo, String titulo,
                                @Schema(description = "Para o selo: ★, 3, 1½...") String numero, String unidade,
                                @Schema(description = "Data do mêsversário") LocalDate dataPrevista,
                                Situacao situacao,
                                @Schema(description = "Dias até o mêsversário (negativo = já passou)") Integer dias,
                                UUID registroId, LocalDate data, String texto,
                                @Schema(description = "Foto da capa (a última do registro)") FotoResponse capa,
                                int quantidadeFotos, Double pesoKg) {
    }

    @Schema(description = "Aviso de mêsversário chegando, hoje ou que passou sem foto")
    public record Chamada(String chave, String titulo, String numero, String unidade, LocalDate data,
                          @Schema(description = "Dias até a data; 0 = hoje; negativo = passou e ainda não tem foto") int dias) {
    }

    @Schema(description = "O álbum da criança")
    public record AlbumResponse(UUID criancaId, int totalFotos, int mesversariosComFoto, int mesversariosPassados,
                                int primeirasVezesFeitas, Chamada chamada,
                                @Schema(description = "Até 45 dias à frente, mais os que já têm foto") List<LugarResponse> mesversarios,
                                List<LugarResponse> primeirasVezes, List<LugarResponse> especiais) {
    }
}
