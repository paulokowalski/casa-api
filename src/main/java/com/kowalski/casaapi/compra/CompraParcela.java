package com.kowalski.casaapi.compra;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    @Column(name = "numero")
    private Integer numeroParcela;

    @Column(name = "valor")
    private BigDecimal valorParcela;

    @Column(name = "data")
    private LocalDate dataParcela;

    public boolean isUltimaParcela() {
        return this.getCompra().getNumeroParcelas().equals(this.getNumeroParcela());
    }

    public double valorQueFalta() {
        if (isUltimaParcela()) return 0;
        return valorParcela.doubleValue() * (compra.getNumeroParcelas() - numeroParcela);
    }
}
