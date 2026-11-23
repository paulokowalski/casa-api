package com.kowalski.casaapi.pessoa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioLogadoTest {

    private final UsuarioLogado usuarioLogado = new UsuarioLogado();

    private void logar(long id, String nome, Papel papel) {
        SecurityContextHolder.getContext().setAuthentication(new UsuarioAutenticado(id, nome, papel));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usuarioSoAcessaOProprioId() {
        logar(1, "Ana", Papel.USUARIO);

        assertThat(usuarioLogado.pessoaId(null)).isEqualTo(1L);
        assertThat(usuarioLogado.pessoaId(1L)).isEqualTo(1L);
        assertThatThrownBy(() -> usuarioLogado.pessoaId(2L)).isInstanceOf(AccessDeniedException.class);
        assertThat(usuarioLogado.podeAcessar(2L)).isFalse();
        assertThat(usuarioLogado.podeAcessar(null)).isFalse();
    }

    @Test
    void usuarioSoAcessaOProprioNomeIgnorandoMaiusculasEEspacos() {
        logar(1, "Ana Souza", Papel.USUARIO);

        assertThat(usuarioLogado.nomePessoa("  ANA SOUZA ")).isEqualTo("  ANA SOUZA ");
        assertThat(usuarioLogado.nomePessoa(null)).isEqualTo("Ana Souza");
        assertThatThrownBy(() -> usuarioLogado.nomePessoa("BRUNO")).isInstanceOf(AccessDeniedException.class);
        assertThat(usuarioLogado.podeAcessarNome("BRUNO")).isFalse();
        assertThat(usuarioLogado.podeAcessarNome("ana souza")).isTrue();
    }

    @Test
    void adminAcessaQualquerPessoa() {
        logar(1, "Admin", Papel.ADMIN);

        assertThat(usuarioLogado.pessoaId(2L)).isEqualTo(2L);
        assertThat(usuarioLogado.nomePessoa("BRUNO")).isEqualTo("BRUNO");
        assertThat(usuarioLogado.podeAcessar(2L)).isTrue();
        assertThat(usuarioLogado.podeAcessarNome("BRUNO")).isTrue();
    }

    @Test
    void semLoginNegaAcesso() {
        assertThatThrownBy(usuarioLogado::atual).isInstanceOf(AccessDeniedException.class);
    }
}
