package com.kowalski.casaapi.pessoa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

@Entity
@Getter @Setter
public class Pessoa {

    public static final int TAMANHO_MINIMO_SENHA = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String email;

    @JsonIgnore
    @Column(name = "senha_hash", length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Papel papel = Papel.USUARIO;

    @Column(nullable = false)
    private boolean ativo = true;

    public boolean isTemAcesso() {
        return senhaHash != null;
    }

    public boolean ehAdmin() {
        return papel == Papel.ADMIN;
    }

    public void alterarDados(String nome, String email) {
        Assert.hasText(nome, "O nome é obrigatório");
        Assert.hasText(email, "O e-mail é obrigatório");
        this.nome = nome.trim();
        this.email = email.trim();
    }

    public void definirSenha(String novaSenha, PasswordEncoder encoder) {
        Assert.isTrue(novaSenha != null && novaSenha.length() >= TAMANHO_MINIMO_SENHA,
                "A senha precisa ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        this.senhaHash = encoder.encode(novaSenha);
    }

    public void tornarAdmin() {
        this.papel = Papel.ADMIN;
        this.ativo = true;
    }
}
