package com.kowalski.casaapi.geracao;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Geracao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Double geracao;

    private Double potencia;

    @Column(name = "data")
    private LocalDateTime data;

    private Geracao(Double geracao, Double potencia, LocalDateTime data) {
        this.geracao = geracao;
        this.potencia = potencia;
        this.data = data;
    }

    public static Geracao registrar(GeradoDTO leitura, LocalDateTime agora) {
        return new Geracao(leitura.gerado(), leitura.potencia(), agora);
    }
}
