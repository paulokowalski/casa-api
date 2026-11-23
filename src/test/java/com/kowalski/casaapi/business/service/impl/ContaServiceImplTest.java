package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.comum.RelogioConfig;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Instant;

import com.kowalski.casaapi.api.v1.input.ContaInput;
import com.kowalski.casaapi.api.v1.input.LancamentoInput;
import com.kowalski.casaapi.api.v1.response.ContaResponse;
import com.kowalski.casaapi.api.v1.response.LancamentoResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.enumerator.TipoConta;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.model.Conta;
import com.kowalski.casaapi.business.model.ContaLancamento;
import com.kowalski.casaapi.business.model.Pessoa;
import com.kowalski.casaapi.business.repository.ContaLancamentoRepository;
import com.kowalski.casaapi.business.repository.ContaRepository;
import com.kowalski.casaapi.business.repository.PessoaRepository;
import com.kowalski.casaapi.business.service.CategoriasService;
import com.kowalski.casaapi.support.Login;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class ContaServiceImplTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), RelogioConfig.FUSO_DA_CASA);

    private final ContaRepository contaRepository = mock(ContaRepository.class);
    private final ContaLancamentoRepository lancamentoRepository = mock(ContaLancamentoRepository.class);
    private final PessoaRepository pessoaRepository = mock(PessoaRepository.class);
    private final CategoriasService categoriasService = mock(CategoriasService.class);
    private final ContaServiceImpl service = new ContaServiceImpl(
            contaRepository, lancamentoRepository, pessoaRepository, categoriasService, new UsuarioLogado(), RELOGIO);

    private final Pessoa ana = Login.pessoa(Login.ANA_ID, "ANA");
    private final Pessoa bruno = Login.pessoa(Login.BRUNO_ID, "BRUNO");

    @BeforeEach
    void setUp() {
        Login.comoAna();
        when(pessoaRepository.findById(Login.ANA_ID)).thenReturn(Optional.of(ana));
        when(pessoaRepository.findById(Login.BRUNO_ID)).thenReturn(Optional.of(bruno));
        when(contaRepository.save(any())).thenAnswer(i -> {
            Conta c = i.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
        when(lancamentoRepository.saveAll(anyCollection())).thenAnswer(i -> new ArrayList<>(i.getArgument(0)));
        when(lancamentoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(lancamentoRepository.doPeriodo(anyCollection(), any(), any())).thenReturn(List.of());
        when(lancamentoRepository.findFirstByContaIdAndCompetenciaBeforeAndValorEstimadoFalseOrderByCompetenciaDesc(any(), any()))
                .thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        Login.sair();
    }

    private static ContaInput input(Long pessoaId, TipoConta tipo, String inicio, String fim, Integer parcelaInicial,
                                    Integer total, boolean variavel, boolean debito) {
        return new ContaInput(pessoaId, null, " Financiamento ", tipo, new BigDecimal("100.00"), 31, inicio, fim,
                parcelaInicial, total, null, variavel, debito);
    }

    private Conta conta(Pessoa dono, TipoConta tipo, LocalDate inicio, LocalDate fim) {
        Conta c = new Conta();
        c.setId(UUID.randomUUID());
        c.setPessoa(dono);
        c.setDescricao("CONTA");
        c.setTipo(tipo);
        c.setValor(new BigDecimal("100.00"));
        c.setDiaVencimento(31);
        c.setCompetenciaInicio(inicio);
        c.setCompetenciaFim(fim);
        when(contaRepository.findById(c.getId())).thenReturn(Optional.of(c));
        return c;
    }


    @Test
    void contaNovaGuardaADataDeCadastroDoRelogio() {
        service.criar(input(Login.ANA_ID, TipoConta.RECORRENTE, "2026-05", null, null, null, false, false));

        verify(contaRepository).save(argThat(c -> c.getDataCadastro().equals(LocalDateTime.now(RELOGIO))));
    }

    @Test
    void parceladaCalculaOUltimoMesPelasParcelasQueFaltam() {
        ContaResponse r = service.criar(input(Login.ANA_ID, TipoConta.PARCELADA, "2026-01", null, 24, 48, false, false));

        assertThat(r.descricao()).isEqualTo("Financiamento");
        assertThat(r.competenciaInicio()).isEqualTo("2026-01");
        assertThat(r.competenciaFim()).isEqualTo("2028-01");
        assertThat(r.parcelaInicial()).isEqualTo(24);
        assertThat(r.totalParcelas()).isEqualTo(48);
    }

    @Test
    void parceladaSemParcelaInicialComecaNaPrimeira() {
        ContaResponse r = service.criar(input(Login.ANA_ID, TipoConta.PARCELADA, "2099-01", null, null, 3, false, false));

        assertThat(r.parcelaInicial()).isEqualTo(1);
        assertThat(r.competenciaFim()).isEqualTo("2099-03");
        assertThat(r.parcelasRestantes()).isEqualTo(3);
        assertThat(r.saldoRestante()).isEqualByComparingTo("300.00");
        assertThat(r.parcelaAtual()).isNull();
    }

    @Test
    void unicaTerminaNoMesmoMesERecorrenteRespeitaOFim() {
        assertThat(service.criar(input(Login.ANA_ID, TipoConta.UNICA, "2026-05", "2027-01", null, null, false, false))
                .competenciaFim()).isEqualTo("2026-05");
        assertThat(service.criar(input(Login.ANA_ID, TipoConta.RECORRENTE, "2026-05", null, null, null, false, false))
                .competenciaFim()).isNull();
        assertThatThrownBy(() -> service.criar(input(Login.ANA_ID, TipoConta.RECORRENTE, "2026-05", "2026-04", null, null, false, false)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validaOsDadosDaConta() {
        assertThatThrownBy(() -> service.criar(new ContaInput(Login.ANA_ID, null, "x", TipoConta.UNICA, BigDecimal.ZERO, 10,
                "2026-01", null, null, null, null, false, false))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.criar(new ContaInput(Login.ANA_ID, null, "x", TipoConta.UNICA, BigDecimal.ONE, 32,
                "2026-01", null, null, null, null, false, false))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.criar(new ContaInput(Login.ANA_ID, null, "x", TipoConta.UNICA, BigDecimal.ONE, 10,
                "jan/2026", null, null, null, null, false, false))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.criar(input(Login.ANA_ID, TipoConta.PARCELADA, "2026-01", null, 5, 4, false, false)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(contaRepository, never()).save(any());
    }

    @Test
    void usuarioNaoCriaContaParaOutraPessoaMasAdminSim() {
        assertThatThrownBy(() -> service.criar(input(Login.BRUNO_ID, TipoConta.UNICA, "2026-01", null, null, null, false, false)))
                .isInstanceOf(AccessDeniedException.class);

        Login.comoAdmin();
        assertThat(service.criar(input(Login.BRUNO_ID, TipoConta.UNICA, "2026-01", null, null, null, false, false)).pessoaId())
                .isEqualTo(Login.BRUNO_ID);
    }

    @Test
    void contaDeOutraPessoaNaoEEncontrada() {
        Conta doBruno = conta(bruno, TipoConta.RECORRENTE, LocalDate.of(2026, 1, 1), null);

        assertThatThrownBy(() -> service.excluir(doBruno.getId(), null)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.editar(doBruno.getId(), input(Login.ANA_ID, TipoConta.UNICA, "2026-01", null, null, null, false, false), null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(contaRepository, never()).delete(any());
    }

    @Test
    void excluirSemDataApagaTudoEComDataEncerraAConta() {
        Conta inteira = conta(ana, TipoConta.RECORRENTE, LocalDate.of(2026, 1, 1), null);
        service.excluir(inteira.getId(), null);
        verify(contaRepository).delete(inteira);

        Conta parcial = conta(ana, TipoConta.RECORRENTE, LocalDate.of(2026, 1, 1), null);
        service.excluir(parcial.getId(), LocalDate.of(2026, 6, 1));
        assertThat(parcial.getCompetenciaFim()).isEqualTo(LocalDate.of(2026, 5, 1));
        verify(lancamentoRepository).removerNaoPagosAPartirDe(parcial.getId(), LocalDate.of(2026, 6, 1));
        verify(contaRepository, never()).delete(parcial);
    }

    @Test
    void editarRegeraOsMesesNaoPagos() {
        Conta c = conta(ana, TipoConta.RECORRENTE, LocalDate.of(2026, 1, 1), null);

        service.editar(c.getId(), input(Login.ANA_ID, TipoConta.RECORRENTE, "2026-01", "2026-12", null, null, false, false),
                LocalDate.of(2026, 3, 1));

        verify(lancamentoRepository).removerNaoPagosAPartirDe(c.getId(), LocalDate.of(2026, 3, 1));
        verify(lancamentoRepository).removerNaoPagosForaDoPeriodo(c.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 1));
    }


    @Test
    void geraUmLancamentoPorMesComVencimentoNoUltimoDiaQuandoODiaNaoExiste() {
        Conta c = conta(ana, TipoConta.RECORRENTE, LocalDate.of(2025, 1, 1), null);
        when(contaRepository.ativasNoPeriodo(Login.ANA_ID, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1))).thenReturn(List.of(c));

        List<LancamentoResponse> r = service.lancamentos(Login.ANA_ID, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 1));

        assertThat(r).extracting(LancamentoResponse::competencia).containsExactly("2025-01", "2025-02", "2025-03");
        assertThat(r).extracting(LancamentoResponse::vencimento).containsExactly(
                LocalDate.of(2025, 1, 31), LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 31));
        assertThat(r).allSatisfy(l -> {
            assertThat(l.valor()).isEqualByComparingTo("100.00");
            assertThat(l.paga()).isFalse();
        });
    }

    @Test
    void lancamentoJaExistenteNaoEGeradoDeNovo() {
        Conta c = conta(ana, TipoConta.RECORRENTE, LocalDate.of(2025, 1, 1), null);
        ContaLancamento existente = new ContaLancamento();
        existente.setConta(c);
        existente.setCompetencia(LocalDate.of(2025, 1, 1));
        existente.setVencimento(LocalDate.of(2025, 1, 31));
        existente.setValor(new BigDecimal("77.00"));
        when(contaRepository.ativasNoPeriodo(any(), any(), any())).thenReturn(List.of(c));
        when(lancamentoRepository.doPeriodo(anyCollection(), any(), any())).thenReturn(List.of(existente));

        List<LancamentoResponse> r = service.lancamentos(Login.ANA_ID, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1));

        assertThat(r).extracting(l -> l.valor().toPlainString()).containsExactly("77.00", "100.00");
    }

    @Test
    void parceladaNumeraAsParcelasEVariavelUsaOUltimoValorReal() {
        Conta parcelada = conta(ana, TipoConta.PARCELADA, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 1));
        parcelada.setParcelaInicial(5);
        parcelada.setTotalParcelas(16);
        parcelada.setValorVariavel(true);
        ContaLancamento ultimoReal = new ContaLancamento();
        ultimoReal.setValor(new BigDecimal("88.00"));
        when(lancamentoRepository.findFirstByContaIdAndCompetenciaBeforeAndValorEstimadoFalseOrderByCompetenciaDesc(any(), any()))
                .thenReturn(Optional.of(ultimoReal));
        when(contaRepository.ativasNoPeriodo(any(), any(), any())).thenReturn(List.of(parcelada));

        LancamentoResponse l = service.lancamentos(Login.ANA_ID, LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 1)).get(0);

        assertThat(l.numeroParcela()).isEqualTo(7);
        assertThat(l.totalParcelas()).isEqualTo(16);
        assertThat(l.valor()).isEqualByComparingTo("88.00");
        assertThat(l.valorEstimado()).isTrue();
    }

    @Test
    void debitoAutomaticoVencidoFicaPago() {
        Conta c = conta(ana, TipoConta.RECORRENTE, LocalDate.of(2025, 1, 1), null);
        c.setDebitoAutomatico(true);
        when(contaRepository.ativasNoPeriodo(any(), any(), any())).thenReturn(List.of(c));

        LancamentoResponse l = service.lancamentos(Login.ANA_ID, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1)).get(0);

        assertThat(l.paga()).isTrue();
        assertThat(l.dataPagamento()).isEqualTo(LocalDate.of(2025, 1, 31));
    }

    @Test
    void periodoDeConsultaEValidado() {
        assertThatThrownBy(() -> service.lancamentos(Login.ANA_ID, LocalDate.of(2025, 5, 1), LocalDate.of(2025, 4, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.lancamentos(Login.ANA_ID, LocalDate.of(2020, 1, 1), LocalDate.of(2023, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void marcarComoPagoUsaHojeSemDataEValorRealAtualizaEstimativas() {
        Conta c = conta(ana, TipoConta.RECORRENTE, LocalDate.of(2025, 1, 1), null);
        c.setValorVariavel(true);
        ContaLancamento l = new ContaLancamento();
        l.setId(UUID.randomUUID());
        l.setConta(c);
        l.setCompetencia(LocalDate.of(2025, 2, 1));
        l.setVencimento(LocalDate.of(2025, 2, 28));
        l.setValor(BigDecimal.TEN);
        l.setValorEstimado(true);
        when(lancamentoRepository.findById(l.getId())).thenReturn(Optional.of(l));

        LancamentoResponse r = service.atualizarLancamento(l.getId(), new LancamentoInput(true, new BigDecimal("55.00"), null));

        assertThat(r.paga()).isTrue();
        assertThat(r.dataPagamento()).isEqualTo(LocalDate.now(RELOGIO));
        assertThat(r.valor()).isEqualByComparingTo("55.00");
        assertThat(r.valorEstimado()).isFalse();
        assertThat(l.isBaixaManual()).isTrue();
        verify(lancamentoRepository).atualizarEstimativasDepoisDe(c.getId(), LocalDate.of(2025, 2, 1), new BigDecimal("55.00"));
    }

    @Test
    void naoAtualizaLancamentoDeOutraPessoa() {
        Conta c = conta(bruno, TipoConta.RECORRENTE, LocalDate.of(2025, 1, 1), null);
        ContaLancamento l = new ContaLancamento();
        l.setId(UUID.randomUUID());
        l.setConta(c);
        when(lancamentoRepository.findById(l.getId())).thenReturn(Optional.of(l));

        assertThatThrownBy(() -> service.atualizarLancamento(l.getId(), new LancamentoInput(true, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(lancamentoRepository, never()).save(any());
    }
}
