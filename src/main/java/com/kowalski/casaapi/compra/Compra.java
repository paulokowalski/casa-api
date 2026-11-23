package com.kowalski.casaapi.compra;

import com.kowalski.casaapi.categoria.Categorias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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

    private static final int ESCALA_DECIMAL = 2;

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

    @JsonIgnore
    @OneToMany(mappedBy = "compra", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<CompraParcela> parcelas;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id", nullable = false)
    private TipoLancamento tipoLancamento;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categorias categoria;

    public static Compra nova(DadosDaCompra dados, LocalDateTime agora) {
        Compra compra = new Compra();
        compra.dataCadastro = agora;
        compra.alterar(dados);
        return compra;
    }

    public void alterar(DadosDaCompra dados) {
        this.nomeProduto = dados.nomeProduto().toUpperCase();
        this.tipoLancamento = dados.cartao();
        this.nomeCartao = dados.cartao().getNome();
        this.categoria = dados.categoria();
        this.valorProduto = dados.valorProduto();
        this.nomePessoaCompra = dados.nomePessoa().toUpperCase();
        this.dataCompra = dados.dataCompra();
        this.numeroParcelas = dados.numeroParcelas();
        gerarParcelas();
    }

    private void gerarParcelas() {
        if (parcelas == null) parcelas = new ArrayList<>();
        parcelas.clear();
        BigDecimal valorParcela = valorProduto.divide(BigDecimal.valueOf(numeroParcelas), ESCALA_DECIMAL, RoundingMode.DOWN);
        for (int numero = 1; numero <= numeroParcelas; numero++) {
            parcelas.add(CompraParcela.builder()
                    .compra(this)
                    .numeroParcela(numero)
                    .valorParcela(valorParcela)
                    .dataParcela(dataCompra.plusMonths(numero))
                    .build());
        }
    }
}
