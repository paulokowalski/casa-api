package com.kowalski.casaapi.crescimento.oms;

/** Medidas com padrão de crescimento da OMS (0 a 5 anos). */
public enum Indicador {
    PESO("Peso", "kg", "peso.tsv", true),
    COMPRIMENTO("Comprimento", "cm", "comprimento.tsv", false),
    PERIMETRO("Perímetro cefálico", "cm", "perimetro.tsv", false);

    private final String nome;
    private final String unidade;
    private final String arquivo;
    private final boolean zRestrito;

    Indicador(String nome, String unidade, String arquivo, boolean zRestrito) {
        this.nome = nome;
        this.unidade = unidade;
        this.arquivo = arquivo;
        this.zRestrito = zRestrito;
    }

    public String nome() {
        return nome;
    }

    public String unidade() {
        return unidade;
    }

    String arquivo() {
        return arquivo;
    }

    /** A OMS ajusta o escore-z além de ±3 nos indicadores de peso (a distribuição tem cauda assimétrica). */
    boolean zRestrito() {
        return zRestrito;
    }
}
