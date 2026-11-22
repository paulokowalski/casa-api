package com.kowalski.casaapi.crescimento.oms;

import com.kowalski.casaapi.crescimento.crianca.Sexo;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Padrões de crescimento da OMS (2006) pelo método LMS, com um valor por dia de vida até os 5 anos.
 * As tabelas vêm do pacote oficial "anthro" da OMS (resources/crescimento/oms).
 *
 * <p>Escore-z: {@code z = ((X/M)^L - 1) / (L·S)}; valor para um z: {@code X = M·(1 + L·S·z)^(1/L)}.
 * Nos indicadores de peso, acima de +3 ou abaixo de -3 a OMS usa a distância entre as linhas ±2 e ±3 DP.
 */
@Component
public class TabelaOms {

    /** Escores-z das linhas de percentil usadas nos gráficos. */
    public static final double Z_P3 = -1.880794, Z_P15 = -1.036433, Z_P85 = 1.036433, Z_P97 = 1.880794;

    private record Lms(double l, double m, double s) {
    }

    private final Map<Indicador, Lms[][]> tabelas = new EnumMap<>(Indicador.class);
    private final int ultimoDia;

    public TabelaOms() {
        int ultimo = Integer.MAX_VALUE;
        for (Indicador ind : Indicador.values()) {
            Lms[][] t = ler(ind);
            tabelas.put(ind, t);
            ultimo = Math.min(ultimo, Math.min(t[0].length, t[1].length) - 1);
        }
        this.ultimoDia = ultimo;
    }

    /** Último dia de vida coberto pelas tabelas (1826 = 5 anos). */
    public int ultimoDia() {
        return ultimoDia;
    }

    public boolean cobre(int dias) {
        return dias >= 0 && dias <= ultimoDia;
    }

    /** Escore-z da medida; vazio se a idade estiver fora das tabelas ou o valor for inválido. */
    public OptionalDouble z(Indicador ind, Sexo sexo, int dias, double valor) {
        if (!cobre(dias) || !(valor > 0)) return OptionalDouble.empty();
        Lms p = lms(ind, sexo, dias);
        double z = zLms(p, valor);
        if (ind.zRestrito() && Math.abs(z) > 3) {
            double sinal = Math.signum(z);
            double sd3 = valorLms(p, 3 * sinal);
            double sd2 = valorLms(p, 2 * sinal);
            z = sinal * 3 + (valor - sd3) / Math.abs(sd3 - sd2);
        }
        return OptionalDouble.of(z);
    }

    /** Valor da medida que corresponde ao escore-z, na idade dada (para desenhar as linhas de percentil). */
    public double valor(Indicador ind, Sexo sexo, int dias, double z) {
        if (!cobre(dias)) throw new IllegalArgumentException("Idade fora das tabelas da OMS: " + dias + " dias");
        return valorLms(lms(ind, sexo, dias), z);
    }

    /** Percentil (0 a 100) de um escore-z. */
    public static double percentil(double z) {
        return 100 * Normal.cdf(z);
    }

    private Lms lms(Indicador ind, Sexo sexo, int dias) {
        return tabelas.get(ind)[sexo == Sexo.F ? 1 : 0][dias];
    }

    private static double zLms(Lms p, double x) {
        return Math.abs(p.l) < 1e-9 ? Math.log(x / p.m) / p.s : (Math.pow(x / p.m, p.l) - 1) / (p.l * p.s);
    }

    private static double valorLms(Lms p, double z) {
        return Math.abs(p.l) < 1e-9 ? p.m * Math.exp(p.s * z) : p.m * Math.pow(1 + p.l * p.s * z, 1 / p.l);
    }

    private static Lms[][] ler(Indicador ind) {
        String caminho = "/crescimento/oms/" + ind.arquivo();
        Lms[][] t = new Lms[2][1900];
        int[] tamanho = new int[2];
        try (InputStream in = TabelaOms.class.getResourceAsStream(caminho)) {
            if (in == null) throw new IllegalStateException("Tabela da OMS não encontrada: " + caminho);
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String linha;
            while ((linha = r.readLine()) != null) {
                if (linha.isBlank() || linha.startsWith("#")) continue;
                String[] c = linha.split("\t");
                int sexo = Integer.parseInt(c[0]) - 1;
                int dia = Integer.parseInt(c[1]);
                t[sexo][dia] = new Lms(Double.parseDouble(c[2]), Double.parseDouble(c[3]), Double.parseDouble(c[4]));
                tamanho[sexo] = Math.max(tamanho[sexo], dia + 1);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Lms[][] r = new Lms[2][];
        for (int s = 0; s < 2; s++) {
            r[s] = java.util.Arrays.copyOf(t[s], tamanho[s]);
            for (int d = 0; d < tamanho[s]; d++)
                if (r[s][d] == null) throw new IllegalStateException("Tabela " + caminho + " sem o dia " + d);
        }
        return r;
    }
}
