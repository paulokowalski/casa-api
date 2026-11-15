package com.kowalski.casaapi.business.model;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kowalski.casaapi.business.enumerator.Papel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Schema(description = "Pessoa da casa")
public class Pessoa {

    @Schema(description = "Id", example = "1")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Nome", example = "PAULO")
    private String nome;

    @Schema(description = "E-mail de login", example = "paulo@exemplo.com")
    private String email;

    @JsonIgnore
    @Column(name = "senha_hash", length = 100)
    private String senhaHash;

    @Schema(description = "ADMIN ou USUARIO", example = "USUARIO")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Papel papel = Papel.USUARIO;

    @Schema(description = "Pode entrar no sistema", example = "true")
    @Column(nullable = false)
    private boolean ativo = true;

    @Schema(description = "Tem senha cadastrada (pode fazer login)", example = "true")
    public boolean isTemAcesso() {
        return senhaHash != null;
    }
}
