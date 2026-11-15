package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.ContaInput;
import com.kowalski.casaapi.api.v1.input.LancamentoInput;
import com.kowalski.casaapi.api.v1.response.ContaResponse;
import com.kowalski.casaapi.api.v1.response.LancamentoResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.service.ContaService;
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

import static com.kowalski.casaapi.util.Competencias.parse;
import static com.kowalski.casaapi.util.Competencias.parseOuNulo;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/contas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Contas", description = "Contas fixas (recorrentes, parceladas, únicas) e seus lançamentos mensais")
public class ContaController {

    private final ContaService contaService;
    private final UsuarioLogado usuarioLogado;

    @GetMapping
    @Operation(summary = "Listar contas da pessoa",
            description = "Com parcela atual, parcelas restantes e saldo calculados para o mês corrente.")
    public List<ContaResponse> listar(@Parameter(description = "Id da pessoa", example = "1") @RequestParam Long pessoaId) {
        return contaService.listar(usuarioLogado.pessoaId(pessoaId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar conta")
    @ApiResponse(responseCode = "201", description = "Conta cadastrada")
    @ApiResponse(responseCode = "404", description = "Pessoa ou categoria não encontradas")
    public ContaResponse criar(@RequestBody ContaInput input) {
        return contaService.criar(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar conta",
            description = "Altera a conta e recalcula os lançamentos **não pagos** a partir de `aPartirDe` "
                    + "(padrão: mês atual). Lançamentos já pagos não mudam.")
    @ApiResponse(responseCode = "200", description = "Conta editada")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada (ou de outra pessoa)")
    public ContaResponse editar(@Parameter(description = "Id da conta") @PathVariable UUID id,
                                @RequestBody ContaInput input,
                                @Parameter(description = "Mês (aaaa-mm) a partir do qual a mudança vale", example = "2026-12")
                                @RequestParam(required = false) String aPartirDe) {
        return contaService.editar(id, input, parseOuNulo(aPartirDe, "aPartirDe"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir conta",
            description = "Sem `aPartirDe` (ou com o mês de início), apaga a conta e o histórico. "
                    + "Com um mês posterior, encerra a conta no mês anterior e remove só os lançamentos não pagos dali em diante.")
    @ApiResponse(responseCode = "204", description = "Conta excluída ou encerrada")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada (ou de outra pessoa)")
    public void excluir(@Parameter(description = "Id da conta") @PathVariable UUID id,
                        @Parameter(description = "Mês (aaaa-mm) a partir do qual a conta deixa de existir", example = "2027-01")
                        @RequestParam(required = false) String aPartirDe) {
        contaService.excluir(id, parseOuNulo(aPartirDe, "aPartirDe"));
    }

    @GetMapping("/lancamentos")
    @Operation(summary = "Lançamentos do período",
            description = "Um lançamento por conta e mês entre `de` e `ate` (inclusive, no máximo 36 meses). "
                    + "Os meses que ainda não existem são gerados na hora. Contas de valor variável sem valor informado "
                    + "vêm com `valorEstimado = true`. Contas em débito automático já vencidas são marcadas como pagas, "
                    + "a não ser que a baixa tenha sido alterada à mão.")
    public List<LancamentoResponse> lancamentos(
            @Parameter(description = "Id da pessoa", example = "1") @RequestParam Long pessoaId,
            @Parameter(description = "Primeiro mês (aaaa-mm)", example = "2026-01") @RequestParam String de,
            @Parameter(description = "Último mês (aaaa-mm), inclusive", example = "2026-12") @RequestParam String ate) {
        return contaService.lancamentos(usuarioLogado.pessoaId(pessoaId), parse(de, "de"), parse(ate, "ate"));
    }

    @PatchMapping("/lancamentos/{id}")
    @Operation(summary = "Atualizar lançamento",
            description = "Marca como pago/não pago e/ou informa o valor real. Só os campos enviados mudam. "
                    + "Em conta de valor variável, o novo valor passa a ser a estimativa dos meses seguintes ainda estimados.")
    @ApiResponse(responseCode = "200", description = "Lançamento atualizado")
    @ApiResponse(responseCode = "404", description = "Lançamento não encontrado (ou de outra pessoa)")
    public LancamentoResponse atualizarLancamento(@Parameter(description = "Id do lançamento") @PathVariable UUID id,
                                                  @RequestBody LancamentoInput input) {
        return contaService.atualizarLancamento(id, input);
    }
}
