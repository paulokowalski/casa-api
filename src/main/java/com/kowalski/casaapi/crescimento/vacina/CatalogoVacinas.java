package com.kowalski.casaapi.crescimento.vacina;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calendário Nacional de Vacinação (PNI/SUS) da criança até 4 anos. Os ids são estáveis (gravados no banco). Ao
 * atualizar o calendário, suba {@link #VERSAO}; não reaproveite um id com outro significado.
 * As datas seguem a idade real (também para prematuros).
 */
public final class CatalogoVacinas {

    public static final String VERSAO = "pni-2025.07";
    public static final String AVISO = "Calendário Nacional de Vacinação do SUS para crianças até 4 anos, com a meningocócica ACWY aos 12 meses "
            + "(desde julho de 2025). Confira sempre com a caderneta e a unidade de saúde: o esquema pode mudar por estado, campanha "
            + "ou indicação médica. A vacina da gripe é anual.";

    /**
     * Uma dose. {@code meses}: idade recomendada. {@code prazoMaximoDias}: idade máxima para aplicar (só rotavírus);
     * null = sem limite rígido (dose atrasada ainda pode ser aplicada).
     */
    public record Dose(String id, String vacina, String dose, int meses, Integer prazoMaximoDias) {
    }

    public static final List<Dose> DOSES = List.of(
            d("bcg", "BCG", "dose única", 0), d("hepb", "Hepatite B", "ao nascer", 0),
            d("penta1", "Pentavalente", "1ª dose", 2), d("vip1", "VIP (poliomielite)", "1ª dose", 2),
            d("pneumo1", "Pneumocócica 10-valente", "1ª dose", 2), new Dose("rota1", "Rotavírus", "1ª dose", 2, 105),
            d("menc1", "Meningocócica C", "1ª dose", 3),
            d("penta2", "Pentavalente", "2ª dose", 4), d("vip2", "VIP (poliomielite)", "2ª dose", 4),
            d("pneumo2", "Pneumocócica 10-valente", "2ª dose", 4), new Dose("rota2", "Rotavírus", "2ª dose", 4, 239),
            d("menc2", "Meningocócica C", "2ª dose", 5),
            d("penta3", "Pentavalente", "3ª dose", 6), d("vip3", "VIP (poliomielite)", "3ª dose", 6),
            d("covid1", "Covid-19", "1ª dose", 6), d("flu1", "Influenza (gripe)", "1ª dose (anual)", 6),
            d("covid2", "Covid-19", "2ª dose", 7), d("flu2", "Influenza (gripe)", "2ª dose (só na primeira vez)", 7),
            d("fa1", "Febre amarela", "1ª dose", 9), d("covid3", "Covid-19", "3ª dose (conforme a vacina usada)", 9),
            d("pneumoR", "Pneumocócica 10-valente", "reforço", 12), d("acwy", "Meningocócica ACWY", "reforço", 12),
            d("scr1", "Tríplice viral (sarampo, caxumba, rubéola)", "1ª dose", 12),
            d("dtp1", "DTP (tríplice bacteriana)", "1º reforço", 15), d("vipR", "VIP (poliomielite)", "reforço", 15),
            d("hepa", "Hepatite A", "dose única", 15), d("tetra", "Tetra viral (ou tríplice viral + varicela)", "dose única", 15),
            d("dtp2", "DTP (tríplice bacteriana)", "2º reforço", 48), d("faR", "Febre amarela", "reforço", 48),
            d("vari2", "Varicela", "2ª dose", 48));

    private static final Map<String, Dose> POR_ID = DOSES.stream().collect(Collectors.toMap(Dose::id, Function.identity()));

    private CatalogoVacinas() {
    }

    private static Dose d(String id, String vacina, String dose, int meses) {
        return new Dose(id, vacina, dose, meses, null);
    }

    public static Optional<Dose> dose(String id) {
        return Optional.ofNullable(POR_ID.get(id));
    }
}
