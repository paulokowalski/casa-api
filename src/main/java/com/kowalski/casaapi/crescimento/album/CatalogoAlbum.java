package com.kowalski.casaapi.crescimento.album;

import java.time.LocalDate;
import com.kowalski.casaapi.crescimento.crianca.Idade;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Os lugares do álbum. As chaves são estáveis (gravadas no banco e usadas pelos apps):
 * <ul>
 *     <li>{@code nascimento}, {@code mes-1} a {@code mes-11}, {@code mes-18}, {@code ano-1} a {@code ano-18}: mêsversários;</li>
 *     <li>{@code primeira-<id>}: primeiras vezes do catálogo abaixo;</li>
 *     <li>{@code especial-<id>}: momentos que os pais nomeiam (batizado, primeira praia...).</li>
 * </ul>
 */
public final class CatalogoAlbum {

    public enum TipoLugar { MESVERSARIO, PRIMEIRA_VEZ, ESPECIAL }

    /** Um lugar do álbum. Nos mêsversários, {@code meses} é a idade (0 = nascimento); nos demais, null. */
    public record Lugar(String chave, TipoLugar tipo, String titulo, String numero, String unidade, Integer meses) {
        public LocalDate data(LocalDate nascimento) {
            return meses == null ? null : nascimento.plusMonths(meses);
        }
    }

    public static final List<Lugar> PRIMEIRAS_VEZES = List.of(
            primeira("banho", "Primeiro banho"), primeira("sorriso", "Primeiro sorriso"), primeira("risada", "Primeira risada"),
            primeira("passeio", "Primeiro passeio"), primeira("papinha", "Primeira papinha"), primeira("dentinho", "Primeiro dentinho"),
            primeira("sentou", "Sentou sozinho"), primeira("engatinhou", "Engatinhou"), primeira("palavra", "Primeira palavra"),
            primeira("passos", "Primeiros passos"), primeira("cabelo", "Primeiro corte de cabelo"), primeira("natal", "Primeiro Natal"));

    private static final Pattern MES = Pattern.compile("mes-(\\d{1,2})");
    private static final Pattern ANO = Pattern.compile("ano-(\\d{1,2})");
    private static final Pattern ESPECIAL = Pattern.compile("especial-[a-z0-9]{6,20}");

    private CatalogoAlbum() {
    }

    private static Lugar primeira(String id, String titulo) {
        return new Lugar("primeira-" + id, TipoLugar.PRIMEIRA_VEZ, titulo, null, null, null);
    }

    static Lugar nascimento() {
        return new Lugar("nascimento", TipoLugar.MESVERSARIO, "Nascimento", "★", "dia 1", 0);
    }

    static Lugar mes(int m) {
        if (m == 18) return new Lugar("mes-18", TipoLugar.MESVERSARIO, "1 ano e meio", "1½", "ano", 18);
        return new Lugar("mes-" + m, TipoLugar.MESVERSARIO, m + (m == 1 ? " mês" : " meses"), String.valueOf(m), m == 1 ? "mês" : "meses", m);
    }

    static Lugar ano(int a) {
        return new Lugar("ano-" + a, TipoLugar.MESVERSARIO, a + (a == 1 ? " ano" : " anos"), String.valueOf(a), a == 1 ? "ano" : "anos", a * 12);
    }

    /**
     * Mêsversários até a idade atual e o próximo: nascimento, 1 a 11 meses, 1 ano, 1 ano e meio (depois dos 11
     * meses) e cada aniversário até o seguinte ao atual.
     */
    public static List<Lugar> mesversarios(LocalDate nascimento, LocalDate hoje) {
        int meses = Idade.mesesCompletos(nascimento, hoje);
        List<Lugar> r = new ArrayList<>();
        r.add(nascimento());
        for (int m = 1; m <= 11; m++) r.add(mes(m));
        r.add(ano(1));
        if (meses >= 11) r.add(mes(18));
        for (int a = 2; a <= Math.min(18, meses / 12 + 1); a++) r.add(ano(a));
        return r;
    }

    /** O lugar da chave, se ela for válida; especiais usam o título dado pelos pais. */
    public static Optional<Lugar> lugar(String chave, String tituloEspecial) {
        if (chave == null) return Optional.empty();
        if (chave.equals("nascimento")) return Optional.of(nascimento());
        Matcher m = MES.matcher(chave);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return n >= 1 && n <= 11 || n == 18 ? Optional.of(mes(n)) : Optional.empty();
        }
        m = ANO.matcher(chave);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1));
            return n >= 1 && n <= 18 ? Optional.of(ano(n)) : Optional.empty();
        }
        Optional<Lugar> primeira = PRIMEIRAS_VEZES.stream().filter(l -> l.chave().equals(chave)).findFirst();
        if (primeira.isPresent()) return primeira;
        if (ESPECIAL.matcher(chave).matches())
            return Optional.of(new Lugar(chave, TipoLugar.ESPECIAL, tituloEspecial == null || tituloEspecial.isBlank() ? "Momento especial" : tituloEspecial,
                    null, null, null));
        return Optional.empty();
    }

    /** Texto da etiqueta no diário: "Nascimento", "Mêsversário · 3 meses", "Primeiro sorriso". */
    public static String rotulo(Lugar l) {
        if (l.tipo() == TipoLugar.MESVERSARIO) return l.meses() == 0 ? "Nascimento" : "Mêsversário · " + l.titulo();
        return l.titulo();
    }
}
