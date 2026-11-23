package com.kowalski.casaapi.compra;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Objects;


@Schema(description = "Lista de parcelas")
public record CompraResponse(
        @Schema(description = "Parcelas encontradas") List<CompraParcelaResponse> compras) {
    public static CompraResponse de(List<CompraParcela> compras, String ultimaParcelaSelecionado){
        var compraParcelaResponse = compras.stream().map(CompraResponse::newCompraParcelaResponse).toList();
        if(StringUtils.hasText(ultimaParcelaSelecionado) && !ultimaParcelaSelecionado.equalsIgnoreCase("TODOS")){
            compraParcelaResponse = compraParcelaResponse.stream().filter(compra -> removerAcentos(compra.ultimaParcela()).equalsIgnoreCase(removerAcentos(ultimaParcelaSelecionado))).toList();
        }
        return new CompraResponse(compraParcelaResponse);
    }

    private static CompraParcelaResponse newCompraParcelaResponse(CompraParcela cp) {
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
                cp.valorQueFalta(),
                cp.getCompra().getNomePessoaCompra().toUpperCase(),
                cp.getCompra().getValorProduto().doubleValue(),
                Objects.nonNull(cp.getCompra().getCategoria()) ? cp.getCompra().getCategoria().getNome().toUpperCase() : "");
    }

    private static String removerAcentos(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }
}
