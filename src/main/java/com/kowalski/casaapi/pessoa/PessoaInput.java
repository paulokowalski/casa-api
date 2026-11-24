package com.kowalski.casaapi.pessoa;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados da pessoa")
public record PessoaInput(
        @Schema(description = "Nome, único; é por ele que as compras indicam a pessoa (nomePessoaCompra)", example = "PAULO") String nome,
        @Schema(description = "E-mail de login (único)", example = "paulo@exemplo.com") String email,
        @Schema(description = "Senha de acesso (mínimo 8 caracteres); vazia mantém a atual") String senha,
        @Schema(description = "ADMIN ou USUARIO; nulo mantém o atual (novo = USUARIO)", example = "USUARIO") Papel papel,
        @Schema(description = "Pode entrar no sistema; nulo mantém o atual (novo = true)", example = "true") Boolean ativo) {
}
