package com.kowalski.casaapi.conta;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContaTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 23, 12, 0);
    private static final BigDecimal CEM = new BigDecimal("100.00");

    private static DadosDaConta dados(TipoConta tipo, LocalDate inicio, LocalDate fim, Integer parcelaInicial, Integer total) {
        return new DadosDaConta(" Internet ", tipo, CEM, 10, inicio, fim, parcelaInicial, total, null, false, false);
    }

    @Test
    void parceladaTerminaPelasParcelasQueFaltam() {
        Conta conta = Conta.nova(dados(TipoConta.PARCELADA, LocalDate.of(2026, 1, 1), null, 24, 48), AGORA);

        assertThat(conta.getDescricao()).isEqualTo("Internet");
        assertThat(conta.getCompetenciaFim()).isEqualTo(LocalDate.of(2028, 1, 1));
        assertThat(conta.parcelaNoMes(LocalDate.of(2026, 3, 1))).isEqualTo(26);
        assertThat(conta.getDataCadastro()).isEqualTo(AGORA);
    }

    @Test
    void unicaDuraUmMesERecorrentePodeNaoTerFim() {
        assertThat(Conta.nova(dados(TipoConta.UNICA, LocalDate.of(2026, 5, 1), null, null, null), AGORA).getCompetenciaFim())
                .isEqualTo(LocalDate.of(2026, 5, 1));
        Conta recorrente = Conta.nova(dados(TipoConta.RECORRENTE, LocalDate.of(2026, 5, 1), null, null, null), AGORA);
        assertThat(recorrente.getCompetenciaFim()).isNull();
        assertThat(recorrente.ativaEm(LocalDate.of(2099, 1, 1))).isTrue();
    }

    @Test
    void regrasDosCampos() {
        assertThatThrownBy(() -> Conta.nova(new DadosDaConta(" ", TipoConta.UNICA, CEM, 10, LocalDate.of(2026, 1, 1),
                null, null, null, null, false, false), AGORA)).hasMessage("A descrição é obrigatória");
        assertThatThrownBy(() -> Conta.nova(new DadosDaConta("x".repeat(121), TipoConta.UNICA, CEM, 10,
                LocalDate.of(2026, 1, 1), null, null, null, null, false, false), AGORA))
                .hasMessage("A descrição deve ter até 120 caracteres");
        assertThatThrownBy(() -> Conta.nova(new DadosDaConta("Luz", TipoConta.UNICA, BigDecimal.ZERO, 10,
                LocalDate.of(2026, 1, 1), null, null, null, null, false, false), AGORA))
                .hasMessage("O valor deve ser maior que zero");
        assertThatThrownBy(() -> Conta.nova(new DadosDaConta("Luz", TipoConta.UNICA, CEM, 32,
                LocalDate.of(2026, 1, 1), null, null, null, null, false, false), AGORA))
                .hasMessage("O dia do vencimento deve estar entre 1 e 31");
        assertThatThrownBy(() -> Conta.nova(dados(TipoConta.PARCELADA, LocalDate.of(2026, 1, 1), null, 5, 4), AGORA))
                .hasMessage("A parcela atual deve estar entre 1 e o total de parcelas");
        assertThatThrownBy(() -> Conta.nova(dados(TipoConta.RECORRENTE, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1), null, null), AGORA))
                .hasMessage("O último mês deve ser depois do primeiro");
    }

    @Test
    void contaInvalidaNaoMudaNada() {
        Conta conta = Conta.nova(dados(TipoConta.RECORRENTE, LocalDate.of(2026, 1, 1), null, null, null), AGORA);

        assertThatThrownBy(() -> conta.alterar(new DadosDaConta("Outra", TipoConta.RECORRENTE, BigDecimal.ONE.negate(), 10,
                LocalDate.of(2027, 1, 1), null, null, null, null, true, true)));

        assertThat(conta.getDescricao()).isEqualTo("Internet");
        assertThat(conta.getCompetenciaInicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(conta.isValorVariavel()).isFalse();
    }

    @Test
    void situacaoDasParcelasAntesDuranteEDepois() {
        Conta conta = Conta.nova(dados(TipoConta.PARCELADA, LocalDate.of(2026, 1, 1), null, 1, 12), AGORA);

        assertThat(conta.situacaoEm(LocalDate.of(2025, 12, 1)))
                .isEqualTo(new SituacaoDasParcelas(null, 12, new BigDecimal("1200.00")));
        assertThat(conta.situacaoEm(LocalDate.of(2026, 4, 1)))
                .isEqualTo(new SituacaoDasParcelas(4, 8, new BigDecimal("800.00")));
        assertThat(conta.situacaoEm(LocalDate.of(2027, 1, 1)))
                .isEqualTo(new SituacaoDasParcelas(null, 0, new BigDecimal("0.00")));
        assertThat(Conta.nova(dados(TipoConta.UNICA, LocalDate.of(2026, 1, 1), null, null, null), AGORA).situacaoEm(LocalDate.of(2026, 1, 1)))
                .isEqualTo(SituacaoDasParcelas.NAO_PARCELADA);
    }

    @Test
    void encerrarSoAdiantaOFim() {
        Conta semFim = Conta.nova(dados(TipoConta.RECORRENTE, LocalDate.of(2026, 1, 1), null, null, null), AGORA);
        semFim.encerrarAntesDe(LocalDate.of(2026, 6, 1));
        assertThat(semFim.getCompetenciaFim()).isEqualTo(LocalDate.of(2026, 5, 1));

        Conta terminaAntes = Conta.nova(dados(TipoConta.RECORRENTE, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), null, null), AGORA);
        terminaAntes.encerrarAntesDe(LocalDate.of(2026, 6, 1));
        assertThat(terminaAntes.getCompetenciaFim()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void lancamentoDeContaVariavelUsaOUltimoValorReal() {
        Conta luz = Conta.nova(new DadosDaConta("Luz", TipoConta.RECORRENTE, CEM, 31, LocalDate.of(2026, 1, 1),
                null, null, null, null, true, false), AGORA);

        ContaLancamento fevereiro = luz.lancamentoDe(LocalDate.of(2026, 2, 1), new BigDecimal("88.00"));
        ContaLancamento semHistorico = luz.lancamentoDe(LocalDate.of(2026, 2, 1), null);

        assertThat(fevereiro.getVencimento()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(fevereiro.getValor()).isEqualByComparingTo("88.00");
        assertThat(fevereiro.isValorEstimado()).isTrue();
        assertThat(semHistorico.getValor()).isEqualByComparingTo("100.00");
    }
}
