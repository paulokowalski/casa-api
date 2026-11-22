package com.kowalski.casaapi.crescimento.foto;

/** Tamanhos gravados de cada foto. Todos em JPEG, sem metadados (a localização GPS some na regravação). */
public enum Variante {
    /** Resolução cheia, para baixar ou imprimir. */
    ORIGINAL(0, 0.92f),
    /** Para ver em tela cheia. */
    MEDIA(1600, 0.85f),
    /** Miniatura das listas e do álbum. */
    MINI(480, 0.80f);

    private final int maiorLado;
    private final float qualidade;

    Variante(int maiorLado, float qualidade) {
        this.maiorLado = maiorLado;
        this.qualidade = qualidade;
    }

    /** 0 = sem redução. */
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
