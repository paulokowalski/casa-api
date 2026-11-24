package com.kowalski.casaapi.servidor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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

    public Double temperaturaMinVidaValida() {
        return temperaturaMinVida == null || temperaturaMinVida <= 0 ? null : temperaturaMinVida;
    }
}
