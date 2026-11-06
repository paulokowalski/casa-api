package com.kowalski.casaapi.util;

import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** Datas de competência (mês de referência), guardadas no banco como o dia 1 do mês. */
public final class Competencias {

    /** Usado no lugar de "sem fim" em comparações */
    public static final LocalDate SEM_FIM = LocalDate.of(9999, 12, 1);

    private Competencias() {
    }

    /** {@code "2026-11"} → 2026-11-01 */
    public static LocalDate parse(String valor, String campo) {
        Assert.hasText(valor, "O campo " + campo + " é obrigatório (formato aaaa-mm)");
        try {
            return YearMonth.parse(valor.trim()).atDay(1);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("O campo " + campo + " deve estar no formato aaaa-mm", e);
        }
    }

    public static LocalDate parseOuNulo(String valor, String campo) {
        return valor == null || valor.isBlank() ? null : parse(valor, campo);
    }

    public static LocalDate de(int ano, int mes) {
        Assert.isTrue(mes >= 1 && mes <= 12, "Mês inválido: " + mes);
        return LocalDate.of(ano, mes, 1);
    }

    public static LocalDate atual() {
        return LocalDate.now().withDayOfMonth(1);
    }

    /** 2026-11-01 → {@code "2026-11"} */
    public static String formatar(LocalDate competencia) {
        return competencia == null ? null : YearMonth.from(competencia).toString();
    }

    public static long mesesEntre(LocalDate de, LocalDate ate) {
        return ChronoUnit.MONTHS.between(de.withDayOfMonth(1), ate.withDayOfMonth(1));
    }

    /** Dia do vencimento no mês; dias que não existem no mês (31 em novembro) viram o último dia. */
    public static LocalDate vencimento(LocalDate competencia, int dia) {
        return competencia.withDayOfMonth(Math.min(dia, competencia.lengthOfMonth()));
    }
}
