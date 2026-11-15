package com.kowalski.casaapi.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Schema(description = "Mensagem de confirmação")
public class MensagemResponse {

    @Schema(description = "Mensagem", example = "Cadastro realizado com sucesso.")
    private String mensagem;

    public MensagemResponse(String mensagem) {
        this.mensagem = mensagem;
    }

}
