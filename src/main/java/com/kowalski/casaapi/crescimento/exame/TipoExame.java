package com.kowalski.casaapi.crescimento.exame;

public enum TipoExame {
    PDF("application/pdf", ".pdf"),
    JPEG("image/jpeg", ".jpg");

    private final String contentType;
    private final String extensao;

    TipoExame(String contentType, String extensao) {
        this.contentType = contentType;
        this.extensao = extensao;
    }

    public String contentType() {
        return contentType;
    }

    public String extensao() {
        return extensao;
    }
}
