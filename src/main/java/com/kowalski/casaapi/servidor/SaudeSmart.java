package com.kowalski.casaapi.servidor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Dados de saúde do SMART de um disco, como o script do host grava (config/smart). Campos nulos: o disco não
 * informa (ex.: NVMe não tem atributos ATA; HD não tem os dados de NVMe).
 *
 * @param lidoEm     quando foi lido; num disco em repouso é a última leitura antes de ele parar
 * @param aprovado   resultado do autoteste do SMART (o próprio disco diz se está falhando)
 * @param atributos  valor bruto dos atributos ATA, pelo número (5 realocados, 197 pendentes, 198 irrecuperáveis,
 *                   187 erros não corrigidos, 199 erros de comunicação, 10 tentativas de giro, 193 ciclos de cabeça)
 * @param falhas     atributos que ficaram abaixo do limite do fabricante, agora ("now") ou no passado ("past")
 */
public record SaudeSmart(OffsetDateTime lidoEm, Boolean aprovado, Long horasLigado, Long vezesLigado,
                         Double temperaturaMinVida, Double temperaturaMaxVida, Double temperaturaLimite,
                         Double temperaturaLimiteAbsoluto, Long vezesAcimaLimite, Map<Integer, Long> atributos,
                         List<Falha> falhas, Nvme nvme) {

    public static final int REALOCADOS = 5;
    public static final int TENTATIVAS_GIRO = 10;
    public static final int ERROS_NAO_CORRIGIDOS = 187;
    public static final int CICLOS_CABECA = 193;
    public static final int PENDENTES = 197;
    public static final int IRRECUPERAVEIS = 198;
    public static final int ERROS_COMUNICACAO = 199;

    public record Falha(String atributo, String quando) {

        public boolean agora() {
            return "now".equalsIgnoreCase(quando);
        }
    }

    public record Nvme(Long avisoCritico, Long reservaDisponivel, Long reservaMinima, Long desgaste, Long errosMidia,
                       Long errosRegistrados, Long desligamentosInesperados, Long minutosAcimaAviso,
                       Long minutosAcimaCritico, Long bytesEscritos, Long bytesLidos) {
    }

    public Long atributo(int id) {
        return atributos == null ? null : atributos.get(id);
    }

    /** Temperatura mínima da vida útil; alguns discos gravam 0 quando não sabem. */
    public Double temperaturaMinVidaValida() {
        return temperaturaMinVida == null || temperaturaMinVida <= 0 ? null : temperaturaMinVida;
    }
}
