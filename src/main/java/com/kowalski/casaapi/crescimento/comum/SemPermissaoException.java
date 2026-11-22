package com.kowalski.casaapi.crescimento.comum;

/** O usuário é da família, mas o papel dele não permite a ação (ex.: avó só com leitura). */
public class SemPermissaoException extends RuntimeException {
    public SemPermissaoException(String mensagem) {
        super(mensagem);
    }
}
