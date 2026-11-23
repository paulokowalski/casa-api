package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraRegistroResponse;
import com.kowalski.casaapi.api.v1.response.CompraResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.categoria.CategoriaService;
import com.kowalski.casaapi.business.model.Compra;
import com.kowalski.casaapi.business.model.CompraParcela;
import com.kowalski.casaapi.business.repository.CompraParcelaRepository;
import com.kowalski.casaapi.business.repository.CompraRepository;
import com.kowalski.casaapi.business.service.TipoLancamentoService;
import com.kowalski.casaapi.business.service.CompraService;
import com.kowalski.casaapi.comum.Competencias;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Clock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {

    private static final String CARTAO_TODOS = "TODOS";
    private static final int ESCALA_DECIMAL = 2;
    static final int MAX_MESES_PERIODO = 36;

    private final CompraRepository compraRepository;
    private final TipoLancamentoService tipoLancamentoService;
    private final CategoriaService categoriaService;
    private final CompraParcelaRepository compraParcelaRepository;
    private final UsuarioLogado usuarioLogado;
    private final Clock relogio;

    @Override
    public List<CompraRegistroResponse> buscarTodos(String pessoa, String cartao) {
        String nome = StringUtils.hasText(pessoa)
                ? usuarioLogado.nomePessoa(pessoa.trim())
                : (usuarioLogado.isAdmin() ? null : usuarioLogado.atual().getNome().trim());
        String cartaoFiltro = StringUtils.hasText(cartao) && !CARTAO_TODOS.equalsIgnoreCase(cartao.trim()) ? cartao.trim() : null;

        if (nome == null && cartaoFiltro == null) return compraRepository.listarRegistros();
        if (nome == null) return compraRepository.listarRegistrosDoCartao(cartaoFiltro);
        if (cartaoFiltro == null) return compraRepository.listarRegistrosDaPessoa(nome);
        return compraRepository.listarRegistrosDaPessoaECartao(nome, cartaoFiltro);
    }

    @Override
    public CompraResponse buscarPorMesENome(String ano, String mes, String pessoa, String cartao, String ultimaParcelaSelecionado) {
        Assert.hasText(ano, "O ano é obrigatório");
        Assert.hasText(mes, "O mês é obrigatório");
        Assert.hasText(pessoa, "A pessoa é obrigatória");
        Assert.hasText(cartao, "O cartão é obrigatório");

        LocalDate inicio = Competencias.de(ano, mes);
        var compras = buscarParcelas(inicio, inicio.plusMonths(1), pessoa, cartao);
        return CompraResponse.de(compras, ultimaParcelaSelecionado);
    }

    @Override
    public CompraResponse buscarPorPeriodo(String de, String ate, String pessoa, String cartao) {
        Assert.hasText(pessoa, "A pessoa é obrigatória");
        LocalDate inicio = Competencias.parse(de, "de");
        LocalDate fim = Competencias.parse(ate, "ate");
        Assert.isTrue(!fim.isBefore(inicio), "O mês final deve ser igual ou posterior ao inicial");
        Assert.isTrue(Competencias.mesesEntre(inicio, fim) < MAX_MESES_PERIODO,
                "O período pode ter no máximo " + MAX_MESES_PERIODO + " meses");

        var compras = buscarParcelas(inicio, fim.plusMonths(1), pessoa, cartao);
        return CompraResponse.de(compras, CARTAO_TODOS);
    }

    @Override
    @Transactional
    public void salvar(CompraInput compraInput) {
        validarCompraInput(compraInput);

        List<CompraInput> pessoas = dividirValorPorItens(compraInput);

        pessoas.forEach(cp -> {
            var compra = criarCompra(cp);
            criarParcelas(cp, compra);
            compraRepository.save(compra);
        });
    }

    public static List<CompraInput> dividirValorPorItens(CompraInput compraInput) {

        List<String> itens = Arrays.stream(compraInput.nomePessoaCompra().split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();

        BigDecimal valorPorItem = compraInput.valorProduto().divide(BigDecimal.valueOf(itens.size()), 2, RoundingMode.HALF_UP);

        return itens.stream()
                .map(item -> compraInput.comNomePessoaEValor(item, valorPorItem))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void remover(UUID id) {
        Assert.notNull(id, "O ID da compra é obrigatório");
        compraRepository.delete(buscarDoUsuario(id));
    }

    private Compra buscarDoUsuario(UUID id) {
        return compraRepository.findById(id)
                .filter(c -> usuarioLogado.podeAcessarNome(c.getNomePessoaCompra()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Compra não encontrada"));
    }

    private List<CompraParcela> buscarParcelas(LocalDate inicio, LocalDate fim, String pessoa, String cartao) {
        if (StringUtils.hasText(cartao) && !CARTAO_TODOS.equalsIgnoreCase(cartao)) {
            return compraParcelaRepository.buscarPorPeriodoECartao(inicio, fim, pessoa, cartao);
        }
        return compraParcelaRepository.buscarPorPeriodo(inicio, fim, pessoa);
    }

    private void validarCompraInput(CompraInput compraInput) {
        Assert.notNull(compraInput, "Os dados da compra são obrigatórios");
        Assert.hasText(compraInput.nomeProduto(), "O nome do produto é obrigatório");
        Assert.notNull(compraInput.tipoLancamentoId(), "O tipo de lançamento é obrigatório");
        Assert.hasText(compraInput.nomePessoaCompra(), "O nome da pessoa é obrigatório");
        Assert.notNull(compraInput.valorProduto(), "O valor do produto é obrigatório");
        Assert.notNull(compraInput.dataCompra(), "A data da compra é obrigatória");
        Assert.isTrue(compraInput.numeroParcelas() > 0, "O número de parcelas deve ser maior que zero");
    }

    private Compra criarCompra(CompraInput compraInput) {
        var tipoLancamento = tipoLancamentoService.findById(compraInput.tipoLancamentoId());
        var categoria = categoriaService.buscar(compraInput.categoriaId());

        return Compra.builder()
                .nomeProduto(compraInput.nomeProduto().toUpperCase())
                .nomeCartao(tipoLancamento.getNome())
                .tipoLancamento(tipoLancamento)
                .categoria(categoria)
                .valorProduto(compraInput.valorProduto())
                .nomePessoaCompra(compraInput.nomePessoaCompra().toUpperCase())
                .dataCompra(compraInput.dataCompra())
                .numeroParcelas(compraInput.numeroParcelas())
                .dataCadastro(LocalDateTime.now(relogio))
                .parcelas(new ArrayList<>())
                .build();
    }

    private void criarParcelas(CompraInput compraInput, Compra compra) {
        BigDecimal valorParcela = calcularValorParcela(compraInput);
        LocalDate dataParcela = compra.getDataCompra();

        for (int numeroParcela = 1; numeroParcela <= compraInput.numeroParcelas(); numeroParcela++) {
            CompraParcela parcela = criarParcela(compra, valorParcela, dataParcela, numeroParcela);
            compra.getParcelas().add(parcela);
            dataParcela = dataParcela.plusMonths(1);
        }
    }

    private BigDecimal calcularValorParcela(CompraInput compraInput) {
        return compraInput.valorProduto()
                .divide(BigDecimal.valueOf(compraInput.numeroParcelas()), ESCALA_DECIMAL, RoundingMode.DOWN);
    }

    private CompraParcela criarParcela(Compra compra, BigDecimal valorParcela, LocalDate dataParcela, int numeroParcela) {
        return CompraParcela.builder()
                .compra(compra)
                .dataParcela(dataParcela.plusMonths(1))
                .numeroParcela(numeroParcela)
                .valorParcela(valorParcela)
                .build();
    }

    @Override
    @Transactional
    public void editar(UUID id, CompraInput compraInput) {
        validarCompraInput(compraInput);

        Compra compra = buscarDoUsuario(id);
        usuarioLogado.nomePessoa(compraInput.nomePessoaCompra());

        var tipoLancamento = tipoLancamentoService.findById(compraInput.tipoLancamentoId());

        compra.setNomeProduto(compraInput.nomeProduto().toUpperCase());
        compra.setTipoLancamento(tipoLancamento);
        compra.setNomeCartao(tipoLancamento.getNome());
        compra.setCategoria(categoriaService.buscar(compraInput.categoriaId()));
        compra.setValorProduto(compraInput.valorProduto());
        compra.setNomePessoaCompra(compraInput.nomePessoaCompra().toUpperCase());
        compra.setDataCompra(compraInput.dataCompra());
        compra.setNumeroParcelas(compraInput.numeroParcelas());

        compraParcelaRepository.deleteByCompraId(compra.getId());
        compra.getParcelas().clear();

        criarParcelas(compraInput, compra);

        compraRepository.save(compra);
    }
}