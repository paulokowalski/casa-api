package com.kowalski.casaapi.api.v1.response;

import com.kowalski.casaapi.api.v1.dto.DatasetDto;
import com.kowalski.casaapi.business.model.Categorias;
import com.kowalski.casaapi.business.model.Compra;
import com.kowalski.casaapi.business.model.CompraParcela;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompraResponseTest {

    private static CompraParcela parcela(String produto, String cartao, int numero, int total, String valor, Categorias cat) {
        Compra compra = Compra.builder().id(UUID.randomUUID()).nomeProduto(produto).nomeCartao(cartao)
                .numeroParcelas(total).nomePessoaCompra("ana").valorProduto(new BigDecimal(valor).multiply(BigDecimal.valueOf(total)))
                .dataCompra(LocalDate.of(2026, 1, 10)).categoria(cat).build();
        return CompraParcela.builder().compra(compra).numeroParcela(numero).valorParcela(new BigDecimal(valor))
                .dataParcela(LocalDate.of(2026, 2, 10)).build();
    }

    private final Categorias mercado = Categorias.builder().nome("Mercado").build();

    @Test
    void montaCadaParcelaComOsCamposDaTela() {
        CompraParcela p = parcela("tv", "nubank", 2, 5, "100.00", mercado);

        CompraParcelaResponse r = new CompraResponse(null, null).to(List.of(p), "TODOS").compras().get(0);

        assertThat(r.id()).isEqualTo(p.getCompra().getId().toString());
        assertThat(r.nomeCompra()).isEqualTo("TV");
        assertThat(r.nomeCartao()).isEqualTo("NUBANK");
        assertThat(r.nomePessoa()).isEqualTo("ANA");
        assertThat(r.categoria()).isEqualTo("MERCADO");
        assertThat(r.valorParcela()).isEqualTo(100.0);
        assertThat(r.valorTotal()).isEqualTo(500.0);
        assertThat(r.numeroParcela()).isEqualTo(2);
        assertThat(r.numeroTotalParcela()).isEqualTo(5);
        assertThat(r.ultimaParcela()).isEqualTo("NÃO");
        assertThat(r.valorFaltante()).isEqualTo(300.0);
    }

    @Test
    void ultimaParcelaNaoTemValorFaltanteECategoriaVaziaViraTextoVazio() {
        CompraParcelaResponse r = new CompraResponse(null, null)
                .to(List.of(parcela("tv", "nubank", 3, 3, "10.00", null)), "TODOS").compras().get(0);

        assertThat(r.ultimaParcela()).isEqualTo("SIM");
        assertThat(r.valorFaltante()).isZero();
        assertThat(r.categoria()).isEmpty();
    }

    @Test
    void filtroDeUltimaParcelaIgnoraAcentos() {
        List<CompraParcela> parcelas = List.of(
                parcela("a", "nubank", 1, 1, "10.00", null),
                parcela("b", "nubank", 1, 2, "20.00", null));

        assertThat(new CompraResponse(null, null).to(parcelas, "SIM").compras())
                .extracting(CompraParcelaResponse::nomeCompra).containsExactly("A");
        assertThat(new CompraResponse(null, null).to(parcelas, "NAO").compras())
                .extracting(CompraParcelaResponse::nomeCompra).containsExactly("B");
        assertThat(new CompraResponse(null, null).to(parcelas, "TODOS").compras()).hasSize(2);
        assertThat(new CompraResponse(null, null).to(parcelas, null).compras()).hasSize(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void graficoSomaAsParcelasPorCartao() {
        List<CompraParcela> parcelas = List.of(
                parcela("a", "nubank", 1, 1, "10.00", null),
                parcela("b", "nubank", 1, 1, "5.50", null),
                parcela("c", "itau", 1, 1, "7.00", null));

        var data = new CompraResponse(null, null).to(parcelas, "TODOS").data();

        List<String> labels = (List<String>) data.get("labels");
        List<Double> valores = ((DatasetDto) ((Object[]) data.get("datasets"))[0]).getData();
        assertThat(labels).containsExactlyInAnyOrder("NUBANK", "ITAU");
        assertThat(valores.get(labels.indexOf("NUBANK"))).isEqualTo(15.5);
        assertThat(valores.get(labels.indexOf("ITAU"))).isEqualTo(7.0);
    }
}
