package com.kowalski.casaapi.servidor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Schema(description = "Saúde do disco pelo SMART, com a avaliação e os contadores que a explicam")
public record ServidorSaudeDiscoResponse(
        @Schema(description = "boa, atencao ou critica", example = "atencao") String situacao,
        @Schema(description = "Motivos da situação, do mais grave ao informativo") List<Alerta> alertas,
        @Schema(description = "Quando o SMART foi lido; num disco em repouso é a última leitura antes de parar") OffsetDateTime lidoEm,
        @Schema(description = "Resultado do autoteste do SMART", example = "true") Boolean aprovado,
        @Schema(description = "Horas ligado", example = "15769") Long horasLigado,
        @Schema(description = "Vezes que foi ligado", example = "112") Long vezesLigado,
        @Schema(description = "Menor temperatura já registrada (°C)", example = "25.0") Double temperaturaMinVida,
        @Schema(description = "Maior temperatura já registrada (°C)", example = "62.0") Double temperaturaMaxVida,
        @Schema(description = "Máximo recomendado pelo fabricante para operar (°C)", example = "60.0") Double temperaturaLimite,
        @Schema(description = "Máximo absoluto do fabricante (°C)", example = "70.0") Double temperaturaLimiteAbsoluto,
        @Schema(description = "Vezes que passou do limite de temperatura", example = "10") Long vezesAcimaLimite,
        @Schema(description = "Setores defeituosos já trocados pela reserva (atributo 5)") Long setoresRealocados,
        @Schema(description = "Setores instáveis aguardando realocação (atributo 197)") Long setoresPendentes,
        @Schema(description = "Setores que não puderam ser lidos (atributo 198)") Long setoresIrrecuperaveis,
        @Schema(description = "Erros de leitura não corrigidos (atributo 187)") Long errosNaoCorrigidos,
        @Schema(description = "Erros de comunicação/CRC (atributo 199): cabo ou adaptador") Long errosComunicacao,
        @Schema(description = "Ciclos de estacionar a cabeça (atributo 193)") Long ciclosCabeca,
        @Schema(description = "Só em NVMe") Nvme nvme) {

    public record Alerta(@Schema(description = "critico, atencao ou info") String nivel, String mensagem) { }

    public record Nvme(
            @Schema(description = "Percentual da vida útil estimada já usado", example = "5") Long desgaste,
            @Schema(description = "Reserva de blocos disponível (%)", example = "100") Long reservaDisponivel,
            @Schema(description = "Reserva mínima antes de alertar (%)", example = "10") Long reservaMinima,
            @Schema(description = "Erros de mídia (dados corrompidos)") Long errosMidia,
            @Schema(description = "Desligamentos sem aviso") Long desligamentosInesperados,
            @Schema(description = "Minutos acima da temperatura de aviso") Long minutosAcimaAviso,
            @Schema(description = "Minutos acima da temperatura crítica") Long minutosAcimaCritico,
            @Schema(description = "Total escrito (bytes)") Long bytesEscritos,
            @Schema(description = "Total lido (bytes)") Long bytesLidos) { }

    public static ServidorSaudeDiscoResponse de(SaudeSmart s, Double temperaturaAtual) {
        if (s == null) return null;
        AvaliacaoDisco.Resultado r = AvaliacaoDisco.avaliar(s, temperaturaAtual);
        SaudeSmart.Nvme n = s.nvme();
        return new ServidorSaudeDiscoResponse(
                r.situacao().name().toLowerCase(Locale.ROOT),
                r.alertas().stream().map(a -> new Alerta(a.nivel().name().toLowerCase(Locale.ROOT), a.mensagem())).toList(),
                s.lidoEm(), s.aprovado(), s.horasLigado(), s.vezesLigado(), s.temperaturaMinVidaValida(),
                s.temperaturaMaxVida(), s.temperaturaLimite(), s.temperaturaLimiteAbsoluto(), s.vezesAcimaLimite(),
                s.atributo(SaudeSmart.REALOCADOS), s.atributo(SaudeSmart.PENDENTES), s.atributo(SaudeSmart.IRRECUPERAVEIS),
                s.atributo(SaudeSmart.ERROS_NAO_CORRIGIDOS), s.atributo(SaudeSmart.ERROS_COMUNICACAO),
                s.atributo(SaudeSmart.CICLOS_CABECA),
                n == null ? null : new Nvme(n.desgaste(), n.reservaDisponivel(), n.reservaMinima(), n.errosMidia(),
                        n.desligamentosInesperados(), n.minutosAcimaAviso(), n.minutosAcimaCritico(), n.bytesEscritos(),
                        n.bytesLidos()));
    }
}
