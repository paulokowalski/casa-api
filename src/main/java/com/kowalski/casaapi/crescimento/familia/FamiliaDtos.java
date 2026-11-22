package com.kowalski.casaapi.crescimento.familia;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FamiliaDtos {

    private FamiliaDtos() {
    }

    @Schema(description = "Dados para criar uma família")
    public record NovaFamilia(@Schema(example = "Família Silva") String nome) {
    }

    @Schema(description = "Família de que o usuário faz parte")
    public record FamiliaResponse(UUID id, String nome,
                                  @Schema(description = "Papel do usuário logado nesta família") PapelFamilia meuPapel,
                                  List<MembroResponse> membros) {
    }

    @Schema(description = "Membro da família")
    public record MembroResponse(Long usuarioId, @Schema(description = "Nome no sistema de login") String nome,
                                 PapelFamilia papel, Instant desde) {
    }

    @Schema(description = "Adicionar ou alterar um membro")
    public record NovoMembro(@Schema(description = "Id do usuário no sistema de login", example = "2") Long usuarioId,
                             @Schema(example = "LEITOR") PapelFamilia papel) {
    }
}
