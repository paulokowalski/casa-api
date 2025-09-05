package com.kowalski.casaapi.business.service.impl;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraResponse;
import com.kowalski.casaapi.business.service.CategoriasService;
import com.kowalski.casaapi.business.model.Compra;
import com.kowalski.casaapi.business.model.CompraParcela;
import com.kowalski.casaapi.business.repository.CompraParcelaRepository;
import com.kowalski.casaapi.business.repository.CompraRepository;
import com.kowalski.casaapi.business.service.CartaoService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {

    private static final String CARTAO_TODOS = "TODOS";
    private static final int ESCALA_DECIMAL = 2;

    private final CompraRepository compraRepository;
    private final CartaoService cartaoService;
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

        var compra = criarCompra(compraInput);
        criarParcelas(compraInput, compra);
        compraRepository.save(compra);
        
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
        Assert.hasText(compraInput.nomeCartao(), "O nome do cartão é obrigatório");
        Assert.hasText(compraInput.nomePessoaCompra(), "O nome da pessoa é obrigatório");
        Assert.notNull(compraInput.valorProduto(), "O valor do produto é obrigatório");
        Assert.notNull(compraInput.dataCompra(), "A data da compra é obrigatória");
        Assert.isTrue(compraInput.numeroParcelas() > 0, "O número de parcelas deve ser maior que zero");
    }

    private Compra criarCompra(CompraInput compraInput) {
        var cartao = cartaoService.findByNome(compraInput.nomeCartao());
        var categoria = categoriasService.findByUUid(compraInput.categoriaId());

        return Compra.builder()
                .id(UUID.randomUUID())
                .nomeProduto(compraInput.nomeProduto().toUpperCase())
                .nomeCartao(cartao.getNome())
                .cartao(cartao)
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

        compra.setNomeProduto(compraInput.nomeProduto().toUpperCase());
        compra.setNomeCartao(compraInput.nomeCartao());
        compra.setCartao(cartaoService.findByNome(compraInput.nomeCartao()));
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