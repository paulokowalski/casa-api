package com.kowalski.casaapi.crescimento.comum;

/** O arquivo enviado não é uma imagem que o servidor consiga ler. */
public class FotoInvalidaException extends RuntimeException {
    public FotoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
