package com.kowalski.casaapi.crescimento.crianca;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Idade da criança: em dias, corrigida para prematuros e por extenso. */
public final class Idade {

    /** Até quando se usa a idade corrigida dos prematuros nas curvas e nos marcos (2 anos). */
    public static final int DIAS_CORRECAO = 730;
    private static final double TERMO_SEMANAS = 40;

    private Idade() {
    }

    public static int dias(LocalDate nascimento, LocalDate data) {
        return (int) ChronoUnit.DAYS.between(nascimento, data);
    }

    public static boolean prematuro(Crianca c) {
        return c.getIdadeGestacionalSemanas() != null && c.getIdadeGestacionalSemanas() < 37;
    }

    /** Quantos dias faltaram para as 40 semanas (0 se não for prematuro). */
    public static int correcao(Crianca c) {
        return prematuro(c) ? (int) Math.round((TERMO_SEMANAS - c.getIdadeGestacionalSemanas()) * 7) : 0;
    }

    /** Idade a usar nas curvas da OMS: corrigida para prematuros até os 2 anos de idade real. */
    public static int diasCurva(Crianca c, LocalDate data) {
        int dias = dias(c.getNascimento(), data);
        return dias < DIAS_CORRECAO ? dias - correcao(c) : dias;
    }

    /** Idade corrigida em dias, ou null se não se aplica (não prematuro ou já passou dos 2 anos). */
    public static Integer diasCorrigidos(Crianca c, LocalDate data) {
        int dias = dias(c.getNascimento(), data);
        return prematuro(c) && dias < DIAS_CORRECAO ? dias - correcao(c) : null;
    }

    /**
     * Meses de vida completos na data, contados como os mêsversários: quem nasceu em 31/01 completa 1 mês em 28/02.
     */
    public static int mesesCompletos(LocalDate nascimento, LocalDate data) {
        if (data.isBefore(nascimento)) return 0;
        Period p = Period.between(nascimento, data);
        int m = p.getYears() * 12 + p.getMonths();
        while (!nascimento.plusMonths(m + 1L).isAfter(data)) m++;
        return m;
    }

    /** Ex.: "7 meses e 3 dias", "1 ano, 2 meses e 5 dias", "12 dias". */
    public static String texto(LocalDate nascimento, LocalDate data) {
        if (data.isBefore(nascimento)) return "antes do nascimento";
        Period p = Period.between(nascimento, data);
        List<String> partes = new ArrayList<>();
        if (p.getYears() > 0) partes.add(p.getYears() + (p.getYears() == 1 ? " ano" : " anos"));
        if (p.getMonths() > 0) partes.add(p.getMonths() + (p.getMonths() == 1 ? " mês" : " meses"));
        if (p.getDays() > 0 || partes.isEmpty()) partes.add(p.getDays() + (p.getDays() == 1 ? " dia" : " dias"));
        if (partes.size() == 1) return partes.get(0);
        return String.join(", ", partes.subList(0, partes.size() - 1)) + " e " + partes.get(partes.size() - 1);
    }
}
