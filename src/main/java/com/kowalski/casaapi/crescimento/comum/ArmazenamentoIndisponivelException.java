package com.kowalski.casaapi.crescimento.comum;

/** O armazenamento das fotos não está acessível (ex.: disco desmontado). */
public class ArmazenamentoIndisponivelException extends RuntimeException {
    public ArmazenamentoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
