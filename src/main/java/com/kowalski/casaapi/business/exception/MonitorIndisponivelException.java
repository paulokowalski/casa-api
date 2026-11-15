package com.kowalski.casaapi.business.exception;

/** O monitoramento não conseguiu ler o sistema ou o banco do histórico (MariaDB). Responde 503. */
public class MonitorIndisponivelException extends RuntimeException {

    public MonitorIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
