package com.kowalski.casaapi.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@Getter @Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nm_produto")
    private String nomeProduto;

    @Column(name = "vl_produto")
    private BigDecimal valorProduto;

    @Column(name = "dt_compra")
    private LocalDate dataCompra;

    @Column(name = "nr_parcelas")
    private Integer numeroParcelas;

    @Column(name = "nm_pessoa_compra")
    private String nomePessoaCompra;

    @Column(name = "nm_cartao")
    private String nomeCartao;

    @Column(name = "dt_cadastrato")
    private LocalDateTime dataCadastro;

    @OneToMany(mappedBy = "compra", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<CompraParcela> parcelas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", nullable = false)
    private Cartao cartao;

}
