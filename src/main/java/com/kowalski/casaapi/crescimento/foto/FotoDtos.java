package com.kowalski.casaapi.crescimento.foto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public final class FotoDtos {

    private FotoDtos() {
    }

    @Schema(description = "Links temporários de cada tamanho (servem direto em <img>, sem login)")
    public record FotoUrls(@Schema(description = "Miniatura, até 480 px") String mini,
                           @Schema(description = "Tela cheia, até 1600 px") String media,
                           @Schema(description = "Resolução cheia") String original,
                           @Schema(description = "Quando os links deixam de valer") Instant expiramEm) {
    }

    @Schema(description = "Foto da criança")
    public record FotoResponse(UUID id, UUID criancaId, int largura, int altura,
                               @Schema(description = "Quando foi tirada (EXIF), se o arquivo informava") LocalDateTime tiradaEm,
                               Instant enviadaEm, Long enviadaPor,
                               @Schema(description = "Registro do diário/álbum em que a foto está; null se solta") UUID registroId,
                               @Schema(description = "true se a mesma foto já tinha sido enviada (não foi duplicada)") boolean jaExistia,
                               FotoUrls urls) {
    }
}
