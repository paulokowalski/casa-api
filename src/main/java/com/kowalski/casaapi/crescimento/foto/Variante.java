package com.kowalski.casaapi.crescimento.foto;

public enum Variante {
    ORIGINAL(0, 0.92f),
    MEDIA(1600, 0.85f),
    MINI(480, 0.80f);

    private final int maiorLado;
    private final float qualidade;

    Variante(int maiorLado, float qualidade) {
        this.maiorLado = maiorLado;
        this.qualidade = qualidade;
    }

    public int maiorLado() {
        return maiorLado;
    }

    public float qualidade() {
        return qualidade;
    }

    public String sufixo() {
        return "-" + name().toLowerCase() + ".jpg";
    }
}
