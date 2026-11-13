package com.kowalski.casaapi.business.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kowalski.casaapi.business.enumerator.Papel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Pessoa {

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
}
