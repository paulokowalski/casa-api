package com.kowalski.casaapi.comum;

import org.springframework.util.Assert;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public final class Competencias {

    public static final LocalDate SEM_FIM = LocalDate.of(9999, 12, 1);

    private Competencias() {
    }

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

    /** Primeiro dia do mês a partir de ano e mês em texto ("2026" e "7" ou "07"). */
    public static LocalDate de(String ano, String mes) {
        Assert.hasText(ano, "O ano é obrigatório");
        Assert.hasText(mes, "O mês é obrigatório");
        try {
            return de(Integer.parseInt(ano.trim()), Integer.parseInt(mes.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Ano e mês devem ser numéricos", e);
        }
    }

    /** Primeiro dia do ano a partir do ano em texto. */
    public static LocalDate inicioDoAno(String ano) {
        return de(ano, "1");
    }

    public static LocalDate atual(Clock relogio) {
        return LocalDate.now(relogio).withDayOfMonth(1);
    }

    public static String formatar(LocalDate competencia) {
        return competencia == null ? null : YearMonth.from(competencia).toString();
    }

    public static long mesesEntre(LocalDate de, LocalDate ate) {
        return ChronoUnit.MONTHS.between(de.withDayOfMonth(1), ate.withDayOfMonth(1));
    }

    public static LocalDate vencimento(LocalDate competencia, int dia) {
        return competencia.withDayOfMonth(Math.min(dia, competencia.lengthOfMonth()));
    }
}
