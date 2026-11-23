package com.kowalski.casaapi.geracao;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "geracao_diaria")
public class GeracaoDiaria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Double geracao;

    @Column(name = "data")
    private LocalDate data;

    GeracaoDiaria(LocalDate data, Double geracao) {
        this.data = data;
        this.geracao = geracao;
    }

    public static GeracaoDiaria doDia(Geracao leitura) {
        return new GeracaoDiaria(leitura.getData().toLocalDate(), leitura.getGeracao());
    }

    public void atualizarTotal(Geracao leitura) {
        this.geracao = leitura.getGeracao();
    }
}
