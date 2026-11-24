package com.kowalski.casaapi.crescimento.comum;

public class SemPermissaoException extends RuntimeException {
    public SemPermissaoException(String mensagem) {
        super(mensagem);
    }
}
