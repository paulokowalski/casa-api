package com.kowalski.casaapi.compra;

import com.kowalski.casaapi.business.MensagemResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/compras", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Compras", description = "Compras no cartão e suas parcelas mensais")
public class CompraController {

    private static final String TODOS = "TODOS";

    private final CompraService compraService;
    private final UsuarioLogado usuarioLogado;

    @GetMapping
    @Operation(summary = "Listar compras",
            description = "Compras sem as parcelas. Sem `pessoa`, o admin recebe as de todos e o usuário só as próprias.")
    public List<CompraRegistroResponse> listar(
            @Parameter(description = "Nome da pessoa (sem diferença de maiúsculas)", example = "PAULO")
            @RequestParam(required = false) String pessoa,
            @Parameter(description = "Nome do cartão; `TODOS` ou vazio não filtra", example = "NUBANK")
            @RequestParam(required = false) String cartao) {
        return compraService.buscarTodos(pessoa, cartao);
    }

    @GetMapping("/parcelas")
    @Operation(summary = "Parcelas de um período",
            description = "Todas as parcelas com vencimento entre `de` e `ate` (inclusive), numa chamada só. "
                    + "O período pode ter no máximo 36 meses. Ordenadas da compra cadastrada mais recente para a mais antiga.")
    public CompraResponse parcelasDoPeriodo(
            @Parameter(description = "Primeiro mês (aaaa-mm)", example = "2026-08") @RequestParam String de,
            @Parameter(description = "Último mês (aaaa-mm), inclusive", example = "2027-11") @RequestParam String ate,
            @Parameter(description = "Nome da pessoa", example = "PAULO") @RequestParam String pessoa,
            @Parameter(description = "Nome do cartão ou `TODOS`", example = "TODOS")
            @RequestParam(defaultValue = TODOS) String cartao) {
        return compraService.buscarPorPeriodo(de, ate, usuarioLogado.nomePessoa(pessoa), cartao);
    }

    @GetMapping("/parcelas/{ano}/{mes}")
    @Operation(summary = "Parcelas de um mês",
            description = "Parcelas que vencem no mês, com filtro opcional de cartão e de última parcela.")
    public CompraResponse parcelasDoMes(
            @Parameter(description = "Ano", example = "2026") @PathVariable String ano,
            @Parameter(description = "Mês (1 a 12, com ou sem zero à esquerda)", example = "11") @PathVariable String mes,
            @Parameter(description = "Nome da pessoa", example = "PAULO") @RequestParam String pessoa,
            @Parameter(description = "Nome do cartão ou `TODOS`", example = "TODOS")
            @RequestParam(defaultValue = TODOS) String cartao,
            @Parameter(description = "`SIM` = só últimas parcelas, `NAO` = sem as últimas, `TODOS` = todas", example = "TODOS")
            @RequestParam(defaultValue = TODOS) String ultimaParcela) {
        return compraService.buscarPorMesENome(ano, mes, usuarioLogado.nomePessoa(pessoa), cartao, ultimaParcela);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar compra",
            description = "Gera as parcelas a partir do mês seguinte à compra. Com várias pessoas em "
                    + "`nomePessoaCompra` (separadas por vírgula), o valor é dividido igualmente e cada uma ganha a sua compra. "
                    + "Valor negativo registra estorno/crédito.")
    @ApiResponse(responseCode = "201", description = "Compra cadastrada")
    @ApiResponse(responseCode = "404", description = "Tipo de lançamento (cartão) ou categoria não encontrados")
    public MensagemResponse criar(@RequestBody CompraInput input) {
        compraService.salvar(input);
        return new MensagemResponse("Cadastro realizado com sucesso.");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar compra", description = "Substitui os dados e recria todas as parcelas.")
    @ApiResponse(responseCode = "200", description = "Compra editada")
    @ApiResponse(responseCode = "404", description = "Compra não encontrada (ou de outra pessoa)")
    public MensagemResponse editar(@Parameter(description = "Id da compra") @PathVariable UUID id,
                                   @RequestBody CompraInput input) {
        compraService.editar(id, input);
        return new MensagemResponse("Compra editada com sucesso.");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir compra", description = "Remove a compra e todas as suas parcelas.")
    @ApiResponse(responseCode = "204", description = "Compra excluída")
    @ApiResponse(responseCode = "404", description = "Compra não encontrada (ou de outra pessoa)")
    public void excluir(@Parameter(description = "Id da compra") @PathVariable UUID id) {
        compraService.remover(id);
    }
}
