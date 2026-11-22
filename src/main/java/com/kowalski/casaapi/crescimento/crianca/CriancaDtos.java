package com.kowalski.casaapi.crescimento.crianca;

import com.kowalski.casaapi.crescimento.foto.FotoDtos.FotoResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

public final class CriancaDtos {

    private CriancaDtos() {
    }

    @Schema(description = "Dados da criança")
    public record CriancaInput(@Schema(example = "Pedro") String nome,
                               @Schema(example = "2026-04-15") LocalDate nascimento,
                               @Schema(example = "M") Sexo sexo,
                               @Schema(description = "Semanas de gestação ao nascer", example = "39") Double idadeGestacionalSemanas,
                               @Schema(example = "3.25") Double pesoNascerKg,
                               @Schema(example = "49.5") Double comprimentoNascerCm,
                               @Schema(example = "34.5") Double perimetroNascerCm,
                               @Schema(description = "Foto de perfil (id de uma foto da criança); null tira a foto") UUID fotoId) {
    }

    @Schema(description = "Criança com a idade calculada para hoje")
    public record CriancaResponse(UUID id, UUID familiaId, String nome, LocalDate nascimento, Sexo sexo,
                                  Double idadeGestacionalSemanas, Double pesoNascerKg, Double comprimentoNascerCm,
                                  Double perimetroNascerCm,
                                  @Schema(description = "Nasceu com menos de 37 semanas") boolean prematuro,
                                  @Schema(description = "Idade em dias") int idadeDias,
                                  @Schema(description = "Idade corrigida em dias (prematuros, até 2 anos); null se não se aplica") Integer idadeCorrigidaDias,
                                  @Schema(example = "7 meses e 3 dias") String idade,
                                  @Schema(description = "Foto de perfil") FotoResponse foto,
                                  @Schema(description = "O usuário logado pode alterar os dados") boolean podeEditar) {
    }
}
