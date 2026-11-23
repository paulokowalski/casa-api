package com.kowalski.casaapi.pessoa;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PessoaTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Test
    void senhaCurtaNaoPode() {
        Pessoa pessoa = new Pessoa();

        assertThatThrownBy(() -> pessoa.definirSenha("1234567", encoder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A senha precisa ter pelo menos 8 caracteres.");
        assertThat(pessoa.isTemAcesso()).isFalse();
    }

    @Test
    void senhaGuardaSoOHash() {
        Pessoa pessoa = new Pessoa();

        pessoa.definirSenha("12345678", encoder);

        assertThat(pessoa.getSenhaHash()).isNotEqualTo("12345678");
        assertThat(encoder.matches("12345678", pessoa.getSenhaHash())).isTrue();
        assertThat(pessoa.isTemAcesso()).isTrue();
    }

    @Test
    void dadosSemEspacosNasPontas() {
        Pessoa pessoa = new Pessoa();

        pessoa.alterarDados(" Ana ", " ana@x.com ");

        assertThat(pessoa.getNome()).isEqualTo("Ana");
        assertThat(pessoa.getEmail()).isEqualTo("ana@x.com");
    }

    @Test
    void nomeEEmailSaoObrigatorios() {
        Pessoa pessoa = new Pessoa();

        assertThatThrownBy(() -> pessoa.alterarDados(" ", "a@x.com")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pessoa.alterarDados("Ana", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tornarAdminTambemAtiva() {
        Pessoa pessoa = new Pessoa();
        pessoa.setAtivo(false);

        pessoa.tornarAdmin();

        assertThat(pessoa.ehAdmin()).isTrue();
        assertThat(pessoa.isAtivo()).isTrue();
    }
}
