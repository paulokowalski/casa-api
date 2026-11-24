package com.kowalski.casaapi.comum;

public class MonitorIndisponivelException extends RuntimeException {

    public MonitorIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
