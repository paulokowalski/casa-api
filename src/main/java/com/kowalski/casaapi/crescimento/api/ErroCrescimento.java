package com.kowalski.casaapi.crescimento.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corpo das respostas de erro do módulo crescimento")
public record ErroCrescimento(@Schema(description = "Mensagem para mostrar ao usuário") String mensagem) {
}
