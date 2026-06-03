package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraResponse;
import com.kowalski.casaapi.business.service.CategoriasService;
import com.kowalski.casaapi.business.model.Compra;
import com.kowalski.casaapi.business.model.CompraParcela;
import com.kowalski.casaapi.business.repository.CompraParcelaRepository;
import com.kowalski.casaapi.business.repository.CompraRepository;
import com.kowalski.casaapi.business.service.TipoLancamentoService;
import com.kowalski.casaapi.business.service.CompraService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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

    private final CompraRepository compraRepository;
    private final TipoLancamentoService tipoLancamentoService;
    private final CategoriasService categoriasService;
    private final CompraParcelaRepository compraParcelaRepository;

    @Override
    public List<Compra> buscarTodos() {
        return compraRepository.findAll();
    }

    @Override
    public CompraResponse buscarPorMesENome(String ano, String mes, String pessoa, String cartao, String ultimaParcelaSelecionado) {
        Assert.hasText(ano, "O ano é obrigatório");
        Assert.hasText(mes, "O mês é obrigatório");
        Assert.hasText(pessoa, "A pessoa é obrigatória");
        Assert.hasText(cartao, "O cartão é obrigatório");

        var compras = buscarComprasFiltradas(ano, mes, pessoa, cartao, ultimaParcelaSelecionado);
        return new CompraResponse(null, null).to(compras, ultimaParcelaSelecionado);
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
        Optional<Compra> compra = compraRepository.findById(id);
        compra.ifPresent(compraRepository::delete);
    }

    private List<CompraParcela> buscarComprasFiltradas(String ano, String mes, String pessoa, String cartao, String ultimaParcelaSelecionado) {
        if (Strings.isNotBlank(ultimaParcelaSelecionado) && !CARTAO_TODOS.equals(cartao)) {
            return compraParcelaRepository.buscarPorMesENomeEPessoaCartao(ano, mes, pessoa, cartao);
        }
        return compraParcelaRepository.buscarPorMesENomeEPessoa(ano, mes, pessoa);
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
        var categoria = categoriasService.findByUUid(compraInput.categoriaId());

        return Compra.builder()
                .id(UUID.randomUUID())
                .nomeProduto(compraInput.nomeProduto().toUpperCase())
                .nomeCartao(tipoLancamento.getNome())
                .tipoLancamento(tipoLancamento)
                .categoria(categoria)
                .valorProduto(compraInput.valorProduto())
                .nomePessoaCompra(compraInput.nomePessoaCompra().toUpperCase())
                .dataCompra(compraInput.dataCompra())
                .numeroParcelas(compraInput.numeroParcelas())
                .dataCadastro(LocalDateTime.now())
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
                .id(UUID.randomUUID())
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

        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Compra não encontrada"));

        var tipoLancamento = tipoLancamentoService.findById(compraInput.tipoLancamentoId());

        compra.setNomeProduto(compraInput.nomeProduto().toUpperCase());
        compra.setTipoLancamento(tipoLancamento);
        compra.setNomeCartao(tipoLancamento.getNome());
        compra.setCategoria(categoriasService.findByUUid(compraInput.categoriaId()));
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