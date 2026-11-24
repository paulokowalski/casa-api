package com.kowalski.casaapi.crescimento.medida;

import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.Idade;
import com.kowalski.casaapi.crescimento.medida.MedidaDtos.*;
import com.kowalski.casaapi.crescimento.oms.Indicador;
import com.kowalski.casaapi.crescimento.oms.TabelaOms;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

@Component
public class AvaliacaoCrescimento {

    private static final double DIAS_MES = 30.4375;

    private final TabelaOms oms;

    public AvaliacaoCrescimento(TabelaOms oms) {
        this.oms = oms;
    }

    public static Function<Medicao, Double> campo(Indicador ind) {
        return switch (ind) {
            case PESO -> Medicao::getPesoKg;
            case COMPRIMENTO -> Medicao::getComprimentoCm;
            case PERIMETRO -> Medicao::getPerimetroCm;
        };
    }

    public static Double aoNascer(Crianca c, Indicador ind) {
        return switch (ind) {
            case PESO -> c.getPesoNascerKg();
            case COMPRIMENTO -> c.getComprimentoNascerCm();
            case PERIMETRO -> c.getPerimetroNascerCm();
        };
    }

    public MedidaAvaliada avaliar(Crianca c, Indicador ind, LocalDate data, Double valor) {
        if (valor == null) return null;
        OptionalDouble z = oms.z(ind, c.getSexo(), Idade.diasCurva(c, data), valor);
        if (z.isEmpty()) return new MedidaAvaliada(valor, null, null, null);
        double p = TabelaOms.percentil(z.getAsDouble());
        return new MedidaAvaliada(valor, arred(z.getAsDouble(), 2), arred(p, 1), faixa(p));
    }

    public MedicaoResponse resposta(Crianca c, Medicao m) {
        Double imc = m.getPesoKg() != null && m.getComprimentoCm() != null
                ? arred(m.getPesoKg() / Math.pow(m.getComprimentoCm() / 100, 2), 1) : null;
        return new MedicaoResponse(m.getId(), m.getData(), Idade.dias(c.getNascimento(), m.getData()),
                Idade.texto(c.getNascimento(), m.getData()),
                avaliar(c, Indicador.PESO, m.getData(), m.getPesoKg()),
                avaliar(c, Indicador.COMPRIMENTO, m.getData(), m.getComprimentoCm()),
                avaliar(c, Indicador.PERIMETRO, m.getData(), m.getPerimetroCm()),
                imc, m.getObservacao(), m.getCriadoPor());
    }

    public List<PontoCrianca> pontos(Crianca c, Indicador ind, List<Medicao> medicoes) {
        List<PontoCrianca> pts = new ArrayList<>();
        Double nasc = aoNascer(c, ind);
        boolean temNoNascimento = medicoes.stream().anyMatch(m -> m.getData().equals(c.getNascimento()) && campo(ind).apply(m) != null);
        if (nasc != null && !temNoNascimento) pts.add(ponto(c, ind, c.getNascimento(), nasc, true));
        medicoes.stream().filter(m -> campo(ind).apply(m) != null).sorted(Comparator.comparing(Medicao::getData))
                .forEach(m -> pts.add(ponto(c, ind, m.getData(), campo(ind).apply(m), false)));
        return pts;
    }

    private PontoCrianca ponto(Crianca c, Indicador ind, LocalDate data, double valor, boolean nascimento) {
        MedidaAvaliada a = avaliar(c, ind, data, valor);
        return new PontoCrianca(data, Idade.diasCurva(c, data), valor, a.z(), a.percentil(), nascimento);
    }

    public CurvaResponse curva(Crianca c, Indicador ind, List<Medicao> medicoes, LocalDate hoje) {
        List<PontoCrianca> pts = pontos(c, ind, medicoes);
        int ultimo = Math.max(Idade.diasCurva(c, hoje), pts.stream().mapToInt(PontoCrianca::dias).max().orElse(0));
        int meses = (int) Math.ceil(ultimo * 1.15 / DIAS_MES);
        int fim = Math.min(oms.ultimoDia(), Math.max(91, (int) Math.round(meses * DIAS_MES)));
        int passo = Math.max(1, fim / 120);
        List<PontoReferencia> ref = new ArrayList<>();
        for (int d = 0; d <= fim; d += passo) ref.add(referencia(c, ind, d));
        if (ref.get(ref.size() - 1).dias() != fim) ref.add(referencia(c, ind, fim));
        return new CurvaResponse(ind, ind.nome(), ind.unidade(), Idade.diasCorrigidos(c, hoje) != null, ref, pts);
    }

    private PontoReferencia referencia(Crianca c, Indicador ind, int d) {
        return new PontoReferencia(d, arred(oms.valor(ind, c.getSexo(), d, TabelaOms.Z_P3), 3),
                arred(oms.valor(ind, c.getSexo(), d, TabelaOms.Z_P15), 3), arred(oms.valor(ind, c.getSexo(), d, 0), 3),
                arred(oms.valor(ind, c.getSexo(), d, TabelaOms.Z_P85), 3), arred(oms.valor(ind, c.getSexo(), d, TabelaOms.Z_P97), 3));
    }

    public ResumoResponse resumo(Crianca c, List<Medicao> medicoes, LocalDate hoje) {
        return resumo(c, medicoes, hoje, null);
    }

    public ResumoResponse resumo(Crianca c, List<Medicao> medicoes, LocalDate hoje, LocalDate proximaConsulta) {
        int idade = Idade.dias(c.getNascimento(), hoje);
        Map<Indicador, UltimaMedida> ultimas = new EnumMap<>(Indicador.class);
        List<Alerta> alertas = new ArrayList<>();
        for (Indicador ind : Indicador.values()) {
            List<PontoCrianca> pts = pontos(c, ind, medicoes);
            if (pts.isEmpty()) continue;
            PontoCrianca u = pts.get(pts.size() - 1);
            ultimas.put(ind, new UltimaMedida(u.data(), u.valor(), u.z(), u.percentil(), u.percentil() == null ? null : faixa(u.percentil())));
            if (u.nascimento()) continue;
            if (u.percentil() != null && (u.percentil() < 3 || u.percentil() > 97))
                alertas.add(new Alerta("FORA_DA_FAIXA", Nivel.ATENCAO, ind, ind.nome() + " fora da faixa P3–P97",
                        "A última medida (" + fmt(u.valor()) + " " + ind.unidade() + ") ficou no percentil " + Math.round(u.percentil())
                                + ". Vale mostrar ao pediatra."));
            PontoCrianca a = pts.size() >= 2 ? pts.get(pts.size() - 2) : null;
            if (a != null && a.percentil() != null && u.percentil() != null
                    && Math.abs(canal(u.percentil()) - canal(a.percentil())) >= 2)
                alertas.add(new Alerta("CRUZOU_LINHAS", Nivel.ATENCAO, ind, ind.nome() + " cruzou duas linhas de percentil",
                        "Foi do percentil " + Math.round(a.percentil()) + " para o " + Math.round(u.percentil()) + " entre "
                                + a.data() + " e " + u.data() + ". Uma mudança assim merece conversa com o pediatra."));
        }

        List<Medicao> pesos = medicoes.stream().filter(m -> m.getPesoKg() != null)
                .sorted(Comparator.comparing(Medicao::getData)).toList();
        GanhoPeso ganho = ganho(c, pesos);
        if (pesos.size() >= 2) {
            Medicao a = pesos.get(pesos.size() - 2), b = pesos.get(pesos.size() - 1);
            if (b.getPesoKg() < a.getPesoKg() && Idade.dias(c.getNascimento(), b.getData()) > 14)
                alertas.add(new Alerta("PESO_CAIU", Nivel.ATENCAO, Indicador.PESO, "O peso caiu desde a última pesagem",
                        fmt(a.getPesoKg()) + " kg → " + fmt(b.getPesoKg()) + " kg. Confirme na próxima pesagem e comente com o pediatra."));
            else if (ganho != null && ganho.dias() >= 14 && "ABAIXO".equals(ganho.situacao()))
                alertas.add(new Alerta("GANHO_BAIXO", Nivel.ATENCAO, Indicador.PESO, "Ganho de peso abaixo do esperado",
                        "Ganhou " + Math.round(ganho.gramasPorDia()) + " g por dia nos últimos " + ganho.dias() + " dias; para a idade, o comum é "
                                + ganho.esperadoMinimo() + " a " + ganho.esperadoMaximo() + " g por dia."));
        }

        Integer desde = pesos.isEmpty() ? null : Idade.dias(pesos.get(pesos.size() - 1).getData(), hoje);
        int intervalo = idade < 183 ? 31 : idade < 365 ? 62 : 120;
        if (desde == null || desde > intervalo)
            alertas.add(new Alerta("PESAGEM_ATRASADA", Nivel.INFO, Indicador.PESO,
                    desde == null ? "Nenhuma pesagem registrada" : "Última pesagem há " + desde + " dias",
                    "Registre uma pesagem para manter a curva em dia."));
        if (Idade.diasCurva(c, hoje) > oms.ultimoDia())
            alertas.add(new Alerta("ALEM_DAS_CURVAS", Nivel.INFO, null, "As curvas da OMS usadas aqui vão até os 5 anos",
                    "Para idades maiores, os percentis não são calculados."));
        alertas.sort(Comparator.comparing(Alerta::nivel));
        return new ResumoResponse(c.getId(), hoje, idade, Idade.diasCorrigidos(c, hoje), Idade.texto(c.getNascimento(), hoje),
                ultimas, ganho, desde, proximaConsulta, alertas);
    }

    GanhoPeso ganho(Crianca c, List<Medicao> pesos) {
        if (pesos.size() < 2) return null;
        Medicao b = pesos.get(pesos.size() - 1);
        Medicao a = null;
        for (int i = pesos.size() - 2; i >= 0; i--) {
            if (Idade.dias(pesos.get(i).getData(), b.getData()) >= 21) { a = pesos.get(i); break; }
        }
        if (a == null) a = pesos.get(pesos.size() - 2);
        int dias = Idade.dias(a.getData(), b.getData());
        if (dias <= 0) return null;
        int gramas = (int) Math.round((b.getPesoKg() - a.getPesoKg()) * 1000);
        double porDia = (double) gramas / dias;
        int[] esperado = ganhoEsperado(Idade.diasCurva(c, b.getData()));
        String situacao = porDia < esperado[0] * 0.6 ? "ABAIXO" : porDia > esperado[1] * 1.6 ? "ACIMA" : "ESPERADO";
        return new GanhoPeso(gramas, dias, arred(porDia, 1), esperado[0], esperado[1], situacao);
    }

    static int[] ganhoEsperado(int dias) {
        double meses = dias / DIAS_MES;
        if (meses < 3) return new int[]{25, 35};
        if (meses < 6) return new int[]{15, 25};
        if (meses < 12) return new int[]{10, 15};
        return new int[]{5, 10};
    }

    static Faixa faixa(double p) {
        if (p < 3) return Faixa.ABAIXO;
        if (p < 15) return Faixa.LIMITE_INFERIOR;
        if (p <= 85) return Faixa.ESPERADA;
        if (p <= 97) return Faixa.LIMITE_SUPERIOR;
        return Faixa.ACIMA;
    }

    static int canal(double p) {
        return p < 3 ? 0 : p < 15 ? 1 : p < 50 ? 2 : p < 85 ? 3 : p < 97 ? 4 : 5;
    }

    private static double arred(double v, int casas) {
        double f = Math.pow(10, casas);
        return Math.round(v * f) / f;
    }

    private static String fmt(double v) {
        return String.format(Locale.forLanguageTag("pt-BR"), v >= 10 ? "%.1f" : "%.2f", v);
    }
}
