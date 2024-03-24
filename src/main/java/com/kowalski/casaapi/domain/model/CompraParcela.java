package com.kowalski.casaapi.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "compra_parcela")
public class CompraParcela {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @Column(name = "nr_parcela")
    private Integer numeroParcela;

    @Column(name = "vl_parcela")
    private BigDecimal valorParcela;

    @Column(name = "dt_parcela")
    private LocalDate dataParcela;

    public boolean isUltimaParcela() {
        return this.getCompra().getNumeroParcelas().equals(this.getNumeroParcela());
    }
}