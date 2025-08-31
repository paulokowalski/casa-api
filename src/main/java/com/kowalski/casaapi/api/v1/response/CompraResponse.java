package com.kowalski.casaapi.api.v1.response;

import com.kowalski.casaapi.api.v1.dto.DatasetDto;
import com.kowalski.casaapi.business.model.CompraParcela;
import io.micrometer.common.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.kowalski.casaapi.util.Util.removerAcentos;

public record CompraResponse (
        List<CompraParcelaResponse> compras,
        Map<String, Object> data
)
{
    public CompraResponse to(List<CompraParcela> compras, String ultimaParcelaSelecionado){
        var compraParcelaResponse = compras.stream().map(this::newCompraParcelaResponse).toList();
        if(StringUtils.isNotBlank(ultimaParcelaSelecionado) && !ultimaParcelaSelecionado.equalsIgnoreCase("TODOS")){
            compraParcelaResponse = compraParcelaResponse.stream().filter(compra -> removerAcentos(compra.ultimaParcela()).equalsIgnoreCase(removerAcentos(ultimaParcelaSelecionado))).toList();
        }
        return new CompraResponse(compraParcelaResponse, montarResponseGrafico(compraParcelaResponse));
    }

    private Map<String, Object> montarResponseGrafico(List<CompraParcelaResponse> compras) {
        Map<String, Object> data = new HashMap<>();

        Map<String, Double> agrupamento = compras.stream().collect(Collectors.groupingBy(CompraParcelaResponse::nomeCartao, Collectors.summingDouble(CompraParcelaResponse::valorParcela)));

        List<String> cartao = new ArrayList<>();
        List<Double> valor = new ArrayList<>();
        agrupamento.forEach((k, v) -> {
            cartao.add(k);
            valor.add(v);
        });

        data.put("labels", cartao);
        data.put("datasets", new Object[]{ new DatasetDto(valor) });
        return data;
    }

    private CompraParcelaResponse newCompraParcelaResponse(CompraParcela cp) {
        return new CompraParcelaResponse(
                cp.getCompra().getId().toString(),
                cp.getCompra().getNomeProduto().toUpperCase(),
                cp.getValorParcela().doubleValue(),
                cp.getDataParcela(),
                cp.getCompra().getDataCompra(),
                cp.getCompra().getNumeroParcelas(),
                cp.getNumeroParcela(),
                cp.isUltimaParcela() ? "SIM" : "NÃO",
                cp.getCompra().getNomeCartao().toUpperCase(),
                calculaValorFalta(cp),
                cp.getCompra().getNomePessoaCompra().toUpperCase(),
                cp.getCompra().getValorProduto().doubleValue(),
                Objects.nonNull(cp.getCompra().getCategoria()) ? cp.getCompra().getCategoria().getNome().toUpperCase() : "");
    }

    private Double calculaValorFalta(CompraParcela cp) {
        if(cp.getCompra().getNumeroParcelas().equals(cp.getNumeroParcela())) return Double.valueOf("0");
        var numeroParcelas = cp.getCompra().getNumeroParcelas() - cp.getNumeroParcela();
        return cp.getValorParcela().doubleValue() * numeroParcelas;
    }
}