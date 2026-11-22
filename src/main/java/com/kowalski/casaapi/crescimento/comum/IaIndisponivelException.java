package com.kowalski.casaapi.crescimento.comum;

/** A IA não está configurada, está ocupada ou falhou; a mensagem é para a família. */
public class IaIndisponivelException extends RuntimeException {
    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
