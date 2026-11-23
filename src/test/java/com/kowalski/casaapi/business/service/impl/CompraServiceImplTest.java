package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.comum.RelogioConfig;
import java.time.Clock;
import java.time.Instant;

import com.kowalski.casaapi.api.v1.response.CompraRegistroResponse;
import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.model.Categorias;
import com.kowalski.casaapi.business.model.Compra;
import com.kowalski.casaapi.business.model.CompraParcela;
import com.kowalski.casaapi.business.model.TipoLancamento;
import com.kowalski.casaapi.business.repository.CompraParcelaRepository;
import com.kowalski.casaapi.business.repository.CompraRepository;
import com.kowalski.casaapi.business.service.CategoriasService;
import com.kowalski.casaapi.business.service.TipoLancamentoService;
import com.kowalski.casaapi.support.Login;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompraServiceImplTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), RelogioConfig.FUSO_DA_CASA);

    private final CompraRepository compraRepository = mock(CompraRepository.class);
    private final TipoLancamentoService tipoService = mock(TipoLancamentoService.class);
    private final CategoriasService categoriasService = mock(CategoriasService.class);
    private final CompraParcelaRepository parcelaRepository = mock(CompraParcelaRepository.class);
    private final CompraServiceImpl service = new CompraServiceImpl(
            compraRepository, tipoService, categoriasService, parcelaRepository, new UsuarioLogado(), RELOGIO);

    private final UUID tipoId = UUID.randomUUID();
    private final UUID categoriaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(tipoService.findById(tipoId)).thenReturn(TipoLancamento.builder().id(tipoId).nome("NUBANK").build());
        when(categoriasService.findByUUid(categoriaId)).thenReturn(Categorias.builder().id(categoriaId).nome("MERCADO").build());
    }

    @AfterEach
    void tearDown() {
        Login.sair();
    }

    private CompraInput input(String pessoas, String valor, int parcelas) {
        return new CompraInput("geladeira", new BigDecimal(valor), LocalDate.of(2026, 1, 15), parcelas,
                pessoas, tipoId, categoriaId);
    }

    private List<Compra> salvas(int vezes) {
        ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
        verify(compraRepository, times(vezes)).save(captor.capture());
        return captor.getAllValues();
    }


    @Test
    void salvarCriaParcelasAPartirDoMesSeguinte() {
        Login.comoAna();
        service.salvar(input("Ana", "100.00", 3));

        Compra compra = salvas(1).get(0);
        assertThat(compra.getNomeProduto()).isEqualTo("GELADEIRA");
        assertThat(compra.getNomePessoaCompra()).isEqualTo("ANA");
        assertThat(compra.getNomeCartao()).isEqualTo("NUBANK");
        assertThat(compra.getValorProduto()).isEqualByComparingTo("100.00");
        assertThat(compra.getParcelas()).extracting(CompraParcela::getNumeroParcela).containsExactly(1, 2, 3);
        assertThat(compra.getParcelas()).extracting(CompraParcela::getDataParcela).containsExactly(
                LocalDate.of(2026, 2, 15), LocalDate.of(2026, 3, 15), LocalDate.of(2026, 4, 15));
        assertThat(compra.getParcelas()).allSatisfy(p -> assertThat(p.getValorParcela()).isEqualByComparingTo("33.33"));
    }

    @Test
    void compraDivididaViraUmaCompraPorPessoaComValorRateado() {
        Login.comoAna();
        service.salvar(input("Ana, Bruno ,", "100.00", 2));

        List<Compra> compras = salvas(2);
        assertThat(compras).extracting(Compra::getNomePessoaCompra).containsExactly("ANA", "BRUNO");
        assertThat(compras).allSatisfy(c -> {
            assertThat(c.getValorProduto()).isEqualByComparingTo("50.00");
            assertThat(c.getParcelas()).hasSize(2)
                    .allSatisfy(p -> assertThat(p.getValorParcela()).isEqualByComparingTo("25.00"));
        });
    }

    @Test
    void rateioArredondaMetadeParaCima() {
        assertThat(CompraServiceImpl.dividirValorPorItens(input("A,B,C", "100.00", 1)))
                .allSatisfy(i -> assertThat(i.valorProduto()).isEqualByComparingTo("33.33"));
        assertThat(CompraServiceImpl.dividirValorPorItens(input("A,B", "0.05", 1)))
                .allSatisfy(i -> assertThat(i.valorProduto()).isEqualByComparingTo("0.03"));
    }

    @Test
    void salvarValidaOsCamposObrigatorios() {
        Login.comoAna();
        assertThatThrownBy(() -> service.salvar(input("Ana", "10", 0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.salvar(input(" ", "10", 1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.salvar(new CompraInput(" ", BigDecimal.TEN, LocalDate.now(RELOGIO), 1, "Ana", tipoId, categoriaId)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(compraRepository, never()).save(any());
    }


    private Compra compraDe(String pessoa) {
        Compra c = Compra.builder().id(UUID.randomUUID()).nomePessoaCompra(pessoa).numeroParcelas(1)
                .parcelas(new ArrayList<>()).build();
        when(compraRepository.findById(c.getId())).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void editarRecriaAsParcelas() {
        Login.comoAna();
        Compra c = compraDe("ANA");

        service.editar(c.getId(), input("Ana", "90.00", 3));

        verify(parcelaRepository).deleteByCompraId(c.getId());
        assertThat(c.getParcelas()).hasSize(3);
        assertThat(c.getValorProduto()).isEqualByComparingTo("90.00");
        verify(compraRepository).save(c);
    }

    @Test
    void naoEditaCompraDeOutraPessoa() {
        Login.comoAna();
        Compra c = compraDe("BRUNO");

        assertThatThrownBy(() -> service.editar(c.getId(), input("Bruno", "10", 1)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(compraRepository, never()).save(any());
    }

    @Test
    void naoPassaACompraParaOutraPessoa() {
        Login.comoAna();
        Compra c = compraDe("ANA");

        assertThatThrownBy(() -> service.editar(c.getId(), input("Bruno", "10", 1)))
                .isInstanceOf(AccessDeniedException.class);
        verify(compraRepository, never()).save(any());
    }

    @Test
    void adminEditaCompraDeQualquerPessoa() {
        Login.comoAdmin();
        Compra c = compraDe("BRUNO");

        service.editar(c.getId(), input("Ana", "10", 1));

        assertThat(c.getNomePessoaCompra()).isEqualTo("ANA");
    }


    @Test
    void removerApagaSoAPropriaCompra() {
        Login.comoAna();
        Compra minha = compraDe("ana");
        Compra outra = compraDe("BRUNO");

        service.remover(minha.getId());
        verify(compraRepository).delete(minha);

        assertThatThrownBy(() -> service.remover(outra.getId())).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(compraRepository, never()).delete(outra);
    }

    @Test
    void removerCompraInexistenteDa404() {
        Login.comoAdmin();
        assertThatThrownBy(() -> service.remover(UUID.randomUUID())).isInstanceOf(RecursoNaoEncontradoException.class);
    }


    @Test
    void adminListaTodasAsCompras() {
        Login.comoAdmin();
        List<CompraRegistroResponse> todas = List.of(registro("ANA"));
        when(compraRepository.listarRegistros()).thenReturn(todas);

        assertThat(service.buscarTodos(null, null)).isSameAs(todas);
        assertThat(service.buscarTodos("", "TODOS")).isSameAs(todas);
    }

    @Test
    void adminFiltraPorPessoaECartaoNoBanco() {
        Login.comoAdmin();
        service.buscarTodos("ANA", null);
        verify(compraRepository).listarRegistrosDaPessoa("ANA");
        service.buscarTodos(null, "NUBANK");
        verify(compraRepository).listarRegistrosDoCartao("NUBANK");
        service.buscarTodos("ANA", "NUBANK");
        verify(compraRepository).listarRegistrosDaPessoaECartao("ANA", "NUBANK");
    }

    @Test
    void usuarioListaSoAsPropriasCompras() {
        Login.comoAna();
        List<CompraRegistroResponse> minhas = List.of(registro("ANA"));
        when(compraRepository.listarRegistrosDaPessoa("ANA")).thenReturn(minhas);

        assertThat(service.buscarTodos(null, null)).isSameAs(minhas);
        verify(compraRepository, never()).listarRegistros();
    }

    @Test
    void usuarioNaoListaComprasDeOutraPessoa() {
        Login.comoAna();
        assertThatThrownBy(() -> service.buscarTodos("BETO", null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void parcelasDoMesFiltramPorPeriodoECartaoSoQuandoInformado() {
        when(parcelaRepository.buscarPorPeriodo(any(), any(), any())).thenReturn(List.of());
        when(parcelaRepository.buscarPorPeriodoECartao(any(), any(), any(), any())).thenReturn(List.of());

        service.buscarPorMesENome("2026", "11", "ANA", "TODOS", "TODOS");
        verify(parcelaRepository).buscarPorPeriodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1), "ANA");

        service.buscarPorMesENome("2026", "12", "ANA", "NUBANK", "TODOS");
        verify(parcelaRepository).buscarPorPeriodoECartao(LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 1), "ANA", "NUBANK");
    }

    @Test
    void mesSemZeroAEsquerdaEquivaleAoComZero() {
        when(parcelaRepository.buscarPorPeriodo(any(), any(), any())).thenReturn(List.of());
        service.buscarPorMesENome("2026", "7", "ANA", "TODOS", "TODOS");
        service.buscarPorMesENome("2026", "07", "ANA", "TODOS", "TODOS");
        verify(parcelaRepository, times(2)).buscarPorPeriodo(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1), "ANA");
    }

    @Test
    void mesInvalidoDaErroDeValidacao() {
        assertThatThrownBy(() -> service.buscarPorMesENome("2026", "13", "ANA", "TODOS", "TODOS"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscarPorMesENome("2026", "xx", "ANA", "TODOS", "TODOS"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void periodoBuscaDoPrimeiroDiaDeDeAteOMesSeguinteDeAte() {
        when(parcelaRepository.buscarPorPeriodo(any(), any(), any())).thenReturn(List.of());
        when(parcelaRepository.buscarPorPeriodoECartao(any(), any(), any(), any())).thenReturn(List.of());

        service.buscarPorPeriodo("2026-08", "2027-11", "ANA", "TODOS");
        verify(parcelaRepository).buscarPorPeriodo(LocalDate.of(2026, 8, 1), LocalDate.of(2027, 12, 1), "ANA");

        service.buscarPorPeriodo("2026-08", "2026-08", "ANA", "NUBANK");
        verify(parcelaRepository).buscarPorPeriodoECartao(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), "ANA", "NUBANK");
    }

    @Test
    void periodoInvertidoOuLongoDemaisERecusado() {
        assertThatThrownBy(() -> service.buscarPorPeriodo("2026-08", "2026-07", "ANA", "TODOS"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscarPorPeriodo("2026-01", "2029-01", "ANA", "TODOS"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.buscarPorPeriodo("2026/01", "2026-02", "ANA", "TODOS"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static CompraRegistroResponse registro(String pessoa) {
        return new CompraRegistroResponse(UUID.randomUUID(), "TV", BigDecimal.TEN, LocalDate.now(RELOGIO), 1, pessoa, "NUBANK", LocalDateTime.now(RELOGIO));
    }
}
