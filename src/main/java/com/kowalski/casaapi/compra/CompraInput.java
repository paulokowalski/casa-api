package com.kowalski.casaapi.compra;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kowalski.casaapi.categoria.Categorias;
import com.kowalski.casaapi.pessoa.Pessoa;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados para cadastrar ou editar uma compra")
public record CompraInput(
        @Schema(description = "Descrição do produto (gravada em maiúsculas)", example = "TV 55 POLEGADAS") String nomeProduto,
        @Schema(description = "Valor total da compra; negativo para estorno/crédito", example = "2400.00") BigDecimal valorProduto,
        @Schema(description = "Data da compra (aaaa-mm-dd); a 1ª parcela vence no mês seguinte", example = "2026-11-15") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dataCompra,
        @Schema(description = "Quantidade de parcelas (1 = à vista)", example = "10") Integer numeroParcelas,
        @Schema(description = "Pessoa ou pessoas (separadas por vírgula) que dividem a compra", example = "PAULO") String nomePessoaCompra,
        @Schema(description = "Id do tipo de lançamento (cartão); ver GET /v1/filtros/tipos-lancamento") UUID tipoLancamentoId,
        @Schema(description = "Id da categoria (opcional); ver GET /v1/filtros/categorias") UUID categoriaId) {

        public CompraInput comNomePessoaEValor(String nomePessoa, BigDecimal valor) {
                return new CompraInput(
                        this.nomeProduto,
                        valor,
                        this.dataCompra,
                        this.numeroParcelas,
                        nomePessoa,
                        this.tipoLancamentoId,
                        this.categoriaId
                );
        }

        public void validar() {
                Assert.hasText(nomeProduto, "O nome do produto é obrigatório");
                Assert.notNull(tipoLancamentoId, "O tipo de lançamento é obrigatório");
                Assert.hasText(nomePessoaCompra, "O nome da pessoa é obrigatório");
                Assert.notNull(valorProduto, "O valor do produto é obrigatório");
                Assert.notNull(dataCompra, "A data da compra é obrigatória");
                Assert.isTrue(numeroParcelas > 0, "O número de parcelas deve ser maior que zero");
        }

        public List<CompraInput> dividirEntrePessoas() {
                List<String> pessoas = Arrays.stream(nomePessoaCompra.split(","))
                        .map(String::trim)
                        .filter(pessoa -> !pessoa.isBlank())
                        .toList();
                BigDecimal valorPorPessoa = valorProduto.divide(BigDecimal.valueOf(pessoas.size()), 2, RoundingMode.HALF_UP);
                return pessoas.stream().map(pessoa -> comNomePessoaEValor(pessoa, valorPorPessoa)).toList();
        }

        public DadosDaCompra paraDados(Pessoa pessoa, TipoLancamento cartao, Categorias categoria) {
                return new DadosDaCompra(nomeProduto, valorProduto, dataCompra, numeroParcelas, pessoa, cartao, categoria);
        }
}
