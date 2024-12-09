package com.kowalski.casaapi.domain.service.impl;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraResponse;
import com.kowalski.casaapi.domain.model.Compra;
import com.kowalski.casaapi.domain.model.CompraParcela;
import com.kowalski.casaapi.domain.repository.CompraParcelaRepository;
import com.kowalski.casaapi.domain.repository.CompraRepository;
import com.kowalski.casaapi.domain.service.CartaoService;
import com.kowalski.casaapi.domain.service.CompraService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompraServiceImpl implements CompraService {

    private final CompraRepository compraRepository;
    private final CartaoService cartaoService;
    private final CompraParcelaRepository compraParcelaRepository;
    private final KafkaTemplate<String, Serializable> kafkaTemplate;

    public List<Compra> buscarTodos(){
        return compraRepository.findAll();
    }

    public CompraResponse buscarPorMesENome(String ano, String mes, String pessoa, String cartao, String ultimaParcelaSelecionado) {
        var lista = Strings.isNotBlank(ultimaParcelaSelecionado) && !cartao.equals("TODOS")?
                compraParcelaRepository.buscarPorMesENomeEPessoaCartao(ano, mes, pessoa, cartao) :
                compraParcelaRepository.buscarPorMesENomeEPessoa(ano, mes, pessoa);
        return new CompraResponse(null, null).to(lista, ultimaParcelaSelecionado);
    }

    @Transactional
    public void salvar(CompraInput compraInput) {
        var compra = criarCompra(compraInput);
        criarParcelas(compraInput, compra);
        compraRepository.save(compra);
        enviarEvento();
    }

    @Override
    public void remover(UUID id) {
        var compra = compraRepository.findById(id);
        compra.ifPresent(compraRepository::delete);
        enviarEvento();
    }

    private Compra criarCompra(CompraInput compraInput) {

        var cartao = cartaoService.findByNome(compraInput.nomeCartao());

        return Compra.builder()
                .id(UUID.randomUUID())
                .nomeProduto(compraInput.nomeProduto().toUpperCase())
                .nomeCartao(cartao.getNome())
                .cartao(cartao)
                .valorProduto(compraInput.valorProduto())
                .nomePessoaCompra(compraInput.nomePessoaCompra().toUpperCase())
                .dataCompra(compraInput.dataCompra())
                .numeroParcelas(compraInput.numeroParcelas())
                .dataCadastro(LocalDateTime.now())
                .build();
    }

    private void criarParcelas(CompraInput compraInput, Compra compra) {
        compra.setParcelas(new ArrayList<>());

        var numeroParcelas = BigDecimal.valueOf(compraInput.numeroParcelas());
        var valorParcela = compraInput.valorProduto().divide(numeroParcelas, 2, RoundingMode.DOWN);

        var dataParcela = compra.getDataCompra();
        for(int x = 0; x < compraInput.numeroParcelas(); x++){
            var compraParcela = CompraParcela.builder()
                    .id(UUID.randomUUID())
                    .compra(compra)
                    .dataParcela(dataParcela.plusMonths(x+1L))
                    .numeroParcela(x+1)
                    .valorParcela(valorParcela)
                    .build();
            compra.getParcelas().add(compraParcela);
        }

    }

    public void enviarEvento(){
        var month = LocalDate.now().getMonth().ordinal() == 11 ? 1 : LocalDate.now().getMonth().ordinal() + 2;
        var year = LocalDate.now().getMonth().ordinal() == 11 ? LocalDate.now().getYear() + 1 : LocalDate.now().getYear();
        var response = compraParcelaRepository.somatorioPorMesENomeEPessoa(String.valueOf(year), String.valueOf(month), "PAULO");
        kafkaTemplate.send("compra-realizada-mensal-ha-topic", response);
    }

}