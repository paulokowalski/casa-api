package com.kowalski.casaapi.crescimento.diario;

import com.kowalski.casaapi.crescimento.foto.FotoDtos.FotoResponse;
import com.kowalski.casaapi.crescimento.medida.MedidaDtos.MedicaoResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class RegistroDtos {

    private RegistroDtos() {
    }

    @Schema(description = "Registro do diário. Envie as fotos antes (POST .../fotos) e passe os ids em fotoIds.")
    public record RegistroInput(@Schema(example = "2026-11-16") LocalDate data,
                                @Schema(example = "MOMENTO") TipoRegistro tipo,
                                @Schema(example = "Rolou sozinho pela primeira vez!") String texto,
                                @Schema(description = "Só em CONSULTA", example = "Dra. Ana, UBS Centro") String profissional,
                                @Schema(description = "Só em CONSULTA: retorno marcado") LocalDate proximaConsulta,
                                @Schema(description = "Lugar no álbum (ex.: mes-3, primeira-sorriso); null = só no diário") String albumChave,
                                @Schema(description = "Nome do momento especial (só para chaves especial-*)") String albumTitulo,
                                Double pesoKg, Double comprimentoCm, Double perimetroCm,
                                @Schema(description = "Fotos do registro, na ordem. Na alteração, null mantém as atuais; fotos que saírem da lista são apagadas.")
                                List<UUID> fotoIds) {
    }

    @Schema(description = "Lugar do álbum de um registro")
    public record AlbumRef(String chave, String titulo, @Schema(example = "Mêsversário · 3 meses") String rotulo) {
    }

    @Schema(description = "Registro do diário")
    public record RegistroResponse(UUID id, UUID criancaId, LocalDate data, int idadeDias, String idade,
                                   @Schema(description = "Mês de vida completo na data (0 = primeiro mês)") int mesDeVida,
                                   TipoRegistro tipo, String texto, String profissional, LocalDate proximaConsulta,
                                   AlbumRef album, MedicaoResponse medicao, List<FotoResponse> fotos,
                                   Long criadoPor, String criadoPorNome, Instant criadoEm, Instant atualizadoEm) {
    }

    @Schema(description = "Item da linha do tempo: um registro do diário ou uma medição avulsa")
    public record ItemLinhaDoTempo(@Schema(example = "REGISTRO") String origem, TipoRegistro tipo, LocalDate data,
                                   int idadeDias, String idade, int mesDeVida,
                                   RegistroResponse registro,
                                   @Schema(description = "Medição avulsa (origem MEDICAO)") MedicaoResponse medicao,
                                   @Schema(description = "Diferença de peso para a pesagem anterior, em gramas") Integer ganhoPesoGramas,
                                   Integer ganhoPesoDias) {
    }

    @Schema(description = "Página da linha do tempo, da mais recente para a mais antiga")
    public record LinhaDoTempoResponse(List<ItemLinhaDoTempo> itens, int pagina, int tamanho, int total, boolean temMais) {
    }
}
