package com.kowalski.casaapi.crescimento.exame;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ExameDtos {

    private ExameDtos() {
    }

    public record ExameInput(String titulo, LocalDate data, String observacao) {
    }

    public record ExameResponse(UUID id, UUID criancaId, String titulo, LocalDate data, String observacao,
                                String nomeArquivo, TipoExame tipo, long bytes, Instant criadoEm, boolean jaExistia) {
    }

    public record ArquivoExame(org.springframework.core.io.Resource conteudo, TipoExame tipo, String nomeArquivo) {
    }
}
