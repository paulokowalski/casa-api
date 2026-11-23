package com.kowalski.casaapi.pessoa;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pessoa da casa")
public record PessoaResponse(
        @Schema(description = "Id", example = "1") Long id,
        @Schema(description = "Nome", example = "PAULO") String nome,
        @Schema(description = "E-mail de login", example = "paulo@exemplo.com") String email,
        @Schema(description = "ADMIN ou USUARIO", example = "USUARIO") Papel papel,
        @Schema(description = "Pode entrar no sistema", example = "true") boolean ativo,
        @Schema(description = "Tem senha cadastrada (pode fazer login)", example = "true") boolean temAcesso) {

    public static PessoaResponse de(Pessoa pessoa) {
        return new PessoaResponse(pessoa.getId(), pessoa.getNome(), pessoa.getEmail(), pessoa.getPapel(),
                pessoa.isAtivo(), pessoa.isTemAcesso());
    }
}
