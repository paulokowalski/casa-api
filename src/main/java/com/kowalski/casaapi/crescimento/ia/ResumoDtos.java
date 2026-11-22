package com.kowalski.casaapi.crescimento.ia;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ResumoDtos {

    private ResumoDtos() {
    }

    @Schema(description = "Resumo de um mês de vida gerado pela IA")
    public record ResumoResponse(@Schema(description = "Mês de vida (0 = primeiro mês)") int mes,
                                 @Schema(example = "4º mês") String rotulo, LocalDate inicio, LocalDate fim,
                                 String texto, List<String> perguntas, String modelo, Instant geradoEm,
                                 @Schema(description = "O mês ainda não terminou quando o resumo foi gerado") boolean parcial,
                                 @Schema(description = "Perguntas feitas à IA sobre este mês, da mais antiga para a mais nova") List<PerguntaResponse> conversa) {
    }

    @Schema(description = "Pergunta dos pais à IA e a resposta")
    public record PerguntaResponse(UUID id, String pergunta, String resposta, Instant feitaEm) {
    }

    @Schema(description = "Pergunta sobre a criança neste mês (crescimento, saúde, cuidados); até 500 caracteres")
    public record PerguntaInput(@Schema(example = "A cólica à tarde é normal nessa idade?") String pergunta) {
    }

    @Schema(description = "Resumos guardados e se a IA está disponível")
    public record ResumosResponse(boolean disponivel,
                                  @Schema(description = "Mês de vida em que a criança está hoje") int mesAtual,
                                  List<ResumoResponse> resumos) {
    }
}
