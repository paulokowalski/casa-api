package com.kowalski.casaapi.domain.service;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraResponse;
import com.kowalski.casaapi.domain.model.Compra;
import com.kowalski.casaapi.domain.model.CompraParcela;
import com.kowalski.casaapi.domain.repository.CompraParcelaRepository;
import com.kowalski.casaapi.domain.repository.CompraRepository;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompraService {

    private final KafkaTemplate<String, Serializable> kafkaTemplate;
    private final CompraRepository compraRepository;
    private final CompraParcelaRepository compraParcelaRepository;

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
    public Compra salvar(CompraInput compraInput) {
        var compra = salvarCompra(compraInput);
        salvarParcelaEnviarEvento(compraInput, compra);
        return compra;
    }

    private Compra salvarCompra(CompraInput compraInput) {
        var compra = Compra.builder()
                .nomeProduto(compraInput.nomeProduto().toUpperCase())
                .nomeCartao(compraInput.nomeCartao().toUpperCase())
                .valorProduto(compraInput.valorProduto())
                .nomePessoaCompra(compraInput.nomePessoaCompra().toUpperCase())
                .dataCompra(compraInput.dataCompra())
                .numeroParcelas(compraInput.numeroParcelas())
                .dataCadastro(LocalDateTime.now())
                .build();
        compraRepository.save(compra);
        return compra;
    }

    private void salvarParcelaEnviarEvento(CompraInput compraInput, Compra compra) {
        LocalDate dtInstallment = compra.getDataCompra();
        for(int x = 0; x < compraInput.numeroParcelas(); x++){
            var big = BigDecimal.valueOf(compraInput.numeroParcelas());
            var compraParcela = CompraParcela.builder()
                    .id(UUID.randomUUID())
                    .compra(compra)
                    .dataParcela(dtInstallment.plusMonths(x+1L))
                    .numeroParcela(x+1)
                    .valorParcela(compraInput.valorProduto().divide(big, 2, RoundingMode.CEILING))
                    .build();
            compraParcelaRepository.save(compraParcela);
        }
        kafkaTemplate.send("compra-realizada-mensal-ha-topic", compraParcelaRepository.somatorioPorMesENomeEPessoa(String.valueOf(compra.getDataCompra().getYear()), String.valueOf(compra.getDataCompra().getMonthValue() + 1), "PAULO"));
    }
}