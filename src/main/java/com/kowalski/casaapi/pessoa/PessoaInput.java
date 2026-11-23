package com.kowalski.casaapi.pessoa;

import io.swagger.v3.oas.annotations.media.Schema;
import com.kowalski.casaapi.pessoa.Papel;

@Schema(description = "Dados da pessoa")
public record PessoaInput(
        @Schema(description = "Nome; é o que liga a pessoa às compras (nomePessoaCompra)", example = "PAULO") String nome,
        @Schema(description = "E-mail de login (único)", example = "paulo@exemplo.com") String email,
        @Schema(description = "Senha de acesso (mínimo 8 caracteres); vazia mantém a atual") String senha,
        @Schema(description = "ADMIN ou USUARIO; nulo mantém o atual (novo = USUARIO)", example = "USUARIO") Papel papel,
        @Schema(description = "Pode entrar no sistema; nulo mantém o atual (novo = true)", example = "true") Boolean ativo) {
}
