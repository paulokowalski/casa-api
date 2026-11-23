package com.kowalski.casaapi.servidor;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Avalia a saúde de um disco a partir do SMART e explica o porquê em português.
 * <ul>
 *   <li><b>crítica</b>: sinais de que o disco está falhando ou perdendo dados — fazer backup e trocar;</li>
 *   <li><b>atenção</b>: desgaste ou defeitos já compensados, superaquecimento — acompanhar;</li>
 *   <li><b>boa</b>: nada relevante (alertas só informativos não mudam a situação).</li>
 * </ul>
 * Os critérios seguem os atributos que mais se relacionam com falha real (realocados, pendentes, irrecuperáveis
 * e erros não corrigidos, como nos estudos da Backblaze) e os limites que o próprio disco informa.
 */
public final class AvaliacaoDisco {

    public enum Nivel { CRITICO, ATENCAO, INFO }

    public enum Situacao { BOA, ATENCAO, CRITICA }

    public record Alerta(Nivel nivel, String mensagem) {
    }

    public record Resultado(Situacao situacao, List<Alerta> alertas) {
    }

    /** Acima disso, setores realocados ou erros não corrigidos deixam de ser "atenção" e viram "crítica". */
    static final long MUITOS = 100;
    static final long MUITOS_ERROS_COMUNICACAO = 100;
    static final long DESGASTE_ATENCAO = 80;

    private static final NumberFormat NUMERO = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"));

    private AvaliacaoDisco() {
    }

    public static Resultado avaliar(SaudeSmart s, Double temperaturaAtual) {
        List<Alerta> alertas = new ArrayList<>();

        if (Boolean.FALSE.equals(s.aprovado())) {
            alertas.add(critico("O próprio disco reprova o teste SMART: faça backup agora e troque o disco."));
        }

        for (SaudeSmart.Falha f : s.falhas() == null ? List.<SaudeSmart.Falha>of() : s.falhas()) {
            boolean temperatura = f.atributo() != null && f.atributo().toLowerCase().contains("temp");
            if (f.agora()) {
                alertas.add(critico(temperatura
                        ? "Temperatura acima do limite do fabricante neste momento."
                        : "O atributo " + f.atributo() + " está abaixo do limite do fabricante agora."));
            } else {
                alertas.add(atencao(temperatura
                        ? "Já passou do limite de temperatura do fabricante (" + f.atributo() + ")."
                        : "O atributo " + f.atributo() + " já ficou abaixo do limite do fabricante no passado."));
            }
        }

        contador(alertas, s.atributo(SaudeSmart.PENDENTES), Nivel.CRITICO, Nivel.CRITICO,
                "%s setor(es) instáveis aguardando realocação: risco de perder dados.");
        contador(alertas, s.atributo(SaudeSmart.IRRECUPERAVEIS), Nivel.CRITICO, Nivel.CRITICO,
                "%s setor(es) que o disco não conseguiu ler.");
        contador(alertas, s.atributo(SaudeSmart.REALOCADOS), Nivel.ATENCAO, Nivel.CRITICO,
                "%s setor(es) defeituosos já trocados pela reserva do disco.");
        contador(alertas, s.atributo(SaudeSmart.ERROS_NAO_CORRIGIDOS), Nivel.ATENCAO, Nivel.CRITICO,
                "%s erro(s) de leitura que o disco não conseguiu corrigir.");
        contador(alertas, s.atributo(SaudeSmart.TENTATIVAS_GIRO), Nivel.ATENCAO, Nivel.ATENCAO,
                "%s tentativa(s) extras para o motor girar.");
        Long crc = s.atributo(SaudeSmart.ERROS_COMUNICACAO);
        if (crc != null && crc > 0) {
            alertas.add(new Alerta(crc >= MUITOS_ERROS_COMUNICACAO ? Nivel.ATENCAO : Nivel.INFO,
                    NUMERO.format(crc) + " erro(s) de comunicação (CRC): costuma ser o cabo ou o adaptador USB, não o disco."));
        }

        temperatura(alertas, s, temperaturaAtual);
        nvme(alertas, s.nvme());

        alertas.sort(Comparator.comparing(Alerta::nivel));
        Situacao situacao = alertas.stream().anyMatch(a -> a.nivel() == Nivel.CRITICO) ? Situacao.CRITICA
                : alertas.stream().anyMatch(a -> a.nivel() == Nivel.ATENCAO) ? Situacao.ATENCAO
                : Situacao.BOA;
        return new Resultado(situacao, List.copyOf(alertas));
    }

    private static void temperatura(List<Alerta> alertas, SaudeSmart s, Double atual) {
        Double limite = s.temperaturaLimite();
        Double absoluto = s.temperaturaLimiteAbsoluto();
        if (atual != null && absoluto != null && atual >= absoluto) {
            alertas.add(critico("Agora em " + graus(atual) + ", no máximo absoluto do fabricante (" + graus(absoluto) + ")."));
        } else if (atual != null && limite != null && atual >= limite) {
            alertas.add(atencao("Agora em " + graus(atual) + ", acima do máximo recomendado (" + graus(limite) + ")."));
        }
        Long vezes = s.vezesAcimaLimite();
        if (vezes != null && vezes > 0) {
            alertas.add(atencao("Passou do limite de temperatura " + NUMERO.format(vezes) + " vez(es)"
                    + (s.temperaturaMaxVida() != null ? "; a máxima já registrada é " + graus(s.temperaturaMaxVida()) : "")
                    + "."));
        }
    }

    private static void nvme(List<Alerta> alertas, SaudeSmart.Nvme n) {
        if (n == null) return;
        if (n.avisoCritico() != null && n.avisoCritico() != 0) {
            alertas.add(critico("O NVMe sinaliza aviso crítico (código " + n.avisoCritico() + ")."));
        }
        if (n.reservaDisponivel() != null && n.reservaMinima() != null && n.reservaDisponivel() < n.reservaMinima()) {
            alertas.add(critico("Reserva de blocos em " + n.reservaDisponivel() + "%, abaixo do mínimo ("
                    + n.reservaMinima() + "%)."));
        }
        contador(alertas, n.errosMidia(), Nivel.CRITICO, Nivel.CRITICO, "%s erro(s) de mídia: dados podem ter sido corrompidos.");
        if (n.desgaste() != null && n.desgaste() >= 100) {
            alertas.add(critico("Vida útil estimada esgotada (desgaste de " + n.desgaste() + "%)."));
        } else if (n.desgaste() != null && n.desgaste() >= DESGASTE_ATENCAO) {
            alertas.add(atencao("Desgaste de " + n.desgaste() + "% da vida útil estimada."));
        }
        contador(alertas, n.minutosAcimaCritico(), Nivel.CRITICO, Nivel.CRITICO,
                "%s minuto(s) acima da temperatura crítica.");
        contador(alertas, n.minutosAcimaAviso(), Nivel.ATENCAO, Nivel.ATENCAO,
                "%s minuto(s) acima da temperatura de aviso.");
        Long quedas = n.desligamentosInesperados();
        if (quedas != null && quedas > 0) {
            alertas.add(new Alerta(Nivel.INFO, NUMERO.format(quedas)
                    + " desligamento(s) sem aviso (queda de energia ou desligar da tomada)."));
        }
    }

    /** Alerta para contador maior que zero; a partir de {@link #MUITOS} usa o nível {@code muitos}. */
    private static void contador(List<Alerta> alertas, Long valor, Nivel nivel, Nivel muitos, String mensagem) {
        if (valor == null || valor <= 0) return;
        alertas.add(new Alerta(valor >= MUITOS ? muitos : nivel, mensagem.formatted(NUMERO.format(valor))));
    }

    private static Alerta critico(String mensagem) {
        return new Alerta(Nivel.CRITICO, mensagem);
    }

    private static Alerta atencao(String mensagem) {
        return new Alerta(Nivel.ATENCAO, mensagem);
    }

    private static String graus(double v) {
        return (v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v).replace('.', ',')) + " °C";
    }
}
