package com.kowalski.casaapi.conta;

import com.kowalski.casaapi.categoria.Categorias;
import com.kowalski.casaapi.pessoa.Pessoa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.kowalski.casaapi.comum.Competencias.SEM_FIM;
import static com.kowalski.casaapi.comum.Competencias.mesesEntre;
import static com.kowalski.casaapi.comum.Competencias.vencimento;

@Entity
@Table(name = "conta")
@Getter
@Setter
public class Conta {

    static final int TAMANHO_MAXIMO_DESCRICAO = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "pessoa_id", nullable = false)
    private Pessoa pessoa;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categorias categoria;

    @Column(nullable = false, length = 120)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoConta tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "dia_vencimento", nullable = false)
    private Integer diaVencimento;

    @Column(name = "competencia_inicio", nullable = false)
    private LocalDate competenciaInicio;

    @Column(name = "competencia_fim")
    private LocalDate competenciaFim;

    @Column(name = "parcela_inicial")
    private Integer parcelaInicial;

    @Column(name = "total_parcelas")
    private Integer totalParcelas;

    @Column(name = "juros_mes", precision = 7, scale = 4)
    private BigDecimal jurosMes;

    @Column(name = "valor_variavel", nullable = false)
    private boolean valorVariavel;

    @Column(name = "debito_automatico", nullable = false)
    private boolean debitoAutomatico;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime dataCadastro;

    public static Conta nova(DadosDaConta dados, LocalDateTime agora) {
        Conta conta = new Conta();
        conta.alterar(dados);
        conta.dataCadastro = agora;
        return conta;
    }

    public void alterar(DadosDaConta dados) {
        Assert.hasText(dados.descricao(), "A descrição é obrigatória");
        Assert.isTrue(dados.descricao().trim().length() <= TAMANHO_MAXIMO_DESCRICAO,
                "A descrição deve ter até " + TAMANHO_MAXIMO_DESCRICAO + " caracteres");
        Assert.notNull(dados.tipo(), "O tipo é obrigatório (RECORRENTE, PARCELADA ou UNICA)");
        Assert.isTrue(dados.valor() != null && dados.valor().signum() > 0, "O valor deve ser maior que zero");
        Assert.isTrue(dados.diaVencimento() != null && dados.diaVencimento() >= 1 && dados.diaVencimento() <= 31,
                "O dia do vencimento deve estar entre 1 e 31");
        Assert.isTrue(dados.jurosMes() == null || dados.jurosMes().signum() >= 0, "Os juros não podem ser negativos");
        Assert.notNull(dados.inicio(), "O primeiro mês é obrigatório");

        Periodo periodo = switch (dados.tipo()) {
            case PARCELADA -> periodoParcelado(dados);
            case UNICA -> new Periodo(dados.inicio(), null, null);
            case RECORRENTE -> periodoRecorrente(dados);
        };

        this.descricao = dados.descricao().trim();
        this.tipo = dados.tipo();
        this.valor = dados.valor();
        this.diaVencimento = dados.diaVencimento();
        this.competenciaInicio = dados.inicio();
        this.competenciaFim = periodo.fim();
        this.parcelaInicial = periodo.parcelaInicial();
        this.totalParcelas = periodo.totalParcelas();
        this.jurosMes = dados.jurosMes();
        this.valorVariavel = dados.valorVariavel();
        this.debitoAutomatico = dados.debitoAutomatico();
    }

    public void vincular(Pessoa dono, Categorias categoria) {
        this.pessoa = Objects.requireNonNull(dono);
        this.categoria = categoria;
    }

    public LocalDate fimOuSemFim() {
        return competenciaFim != null ? competenciaFim : SEM_FIM;
    }

    public boolean ativaEm(LocalDate mes) {
        return competenciaFim == null || !competenciaFim.isBefore(mes);
    }

    public boolean apagadaPorInteiroAoExcluirDesde(LocalDate aPartirDe) {
        return aPartirDe == null || !aPartirDe.isAfter(competenciaInicio);
    }

    public void encerrarAntesDe(LocalDate aPartirDe) {
        LocalDate novoFim = aPartirDe.minusMonths(1);
        if (competenciaFim == null || competenciaFim.isAfter(novoFim)) {
            competenciaFim = novoFim;
        }
    }

    public int parcelaNoMes(LocalDate competencia) {
        return parcelaInicial + (int) mesesEntre(competenciaInicio, competencia);
    }

    public SituacaoDasParcelas situacaoEm(LocalDate mesAtual) {
        if (tipo != TipoConta.PARCELADA) return SituacaoDasParcelas.NAO_PARCELADA;
        if (mesAtual.isBefore(competenciaInicio)) {
            return SituacaoDasParcelas.de(null, totalParcelas - parcelaInicial + 1, valor);
        }
        if (ativaEm(mesAtual)) {
            int atual = parcelaNoMes(mesAtual);
            return SituacaoDasParcelas.de(atual, totalParcelas - atual, valor);
        }
        return SituacaoDasParcelas.de(null, 0, valor);
    }

    public ContaLancamento lancamentoDe(LocalDate competencia, BigDecimal ultimoValorReal) {
        ContaLancamento lancamento = new ContaLancamento();
        lancamento.setConta(this);
        lancamento.setCompetencia(competencia);
        lancamento.setVencimento(vencimento(competencia, diaVencimento));
        if (tipo == TipoConta.PARCELADA) {
            lancamento.setNumeroParcela(parcelaNoMes(competencia));
        }
        if (valorVariavel) {
            lancamento.setValor(Objects.requireNonNullElse(ultimoValorReal, valor));
            lancamento.setValorEstimado(true);
        } else {
            lancamento.setValor(valor);
        }
        return lancamento;
    }

    private static Periodo periodoParcelado(DadosDaConta dados) {
        Assert.isTrue(dados.totalParcelas() != null && dados.totalParcelas() >= 1, "Informe o total de parcelas");
        int inicial = Objects.requireNonNullElse(dados.parcelaInicial(), 1);
        int total = dados.totalParcelas();
        Assert.isTrue(inicial >= 1 && inicial <= total, "A parcela atual deve estar entre 1 e o total de parcelas");
        return new Periodo(dados.inicio().plusMonths((long) total - inicial), inicial, total);
    }

    private static Periodo periodoRecorrente(DadosDaConta dados) {
        Assert.isTrue(dados.fim() == null || !dados.fim().isBefore(dados.inicio()), "O último mês deve ser depois do primeiro");
        return new Periodo(dados.fim(), null, null);
    }

    private record Periodo(LocalDate fim, Integer parcelaInicial, Integer totalParcelas) {
    }
}
