package com.kowalski.casaapi.crescimento.comum;

/** Recurso inexistente ou de outra família (não revela qual dos dois). */
public class NaoEncontradoException extends RuntimeException {
    public NaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
