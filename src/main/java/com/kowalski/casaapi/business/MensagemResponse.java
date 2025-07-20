package com.kowalski.casaapi.business;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MensagemResponse {

    private String mensagem;

    public MensagemResponse(String mensagem) {
        this.mensagem = mensagem;
    }

}
