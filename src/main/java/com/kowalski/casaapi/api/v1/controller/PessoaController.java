package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.PessoaInput;
import com.kowalski.casaapi.business.exception.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.model.Pessoa;
import com.kowalski.casaapi.business.service.PessoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/pessoas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Pessoas", description = "Pessoas da casa e seus acessos. Criar, alterar e excluir é só para ADMIN.")
public class PessoaController {

    private final PessoaService pessoaService;

    private Pessoa buscar(Long id) {
        return pessoaService.buscarPorId(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada"));
    }

    @GetMapping
    @Operation(summary = "Listar pessoas", description = "Ordenadas por nome. A senha nunca é devolvida.")
    public List<Pessoa> listar() {
        return pessoaService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar pessoa (ADMIN)",
            description = "Nome e e-mail são obrigatórios e o e-mail não pode repetir. "
                    + "Com `senha` (mínimo 8 caracteres), a pessoa passa a poder entrar.")
    @ApiResponse(responseCode = "201", description = "Pessoa cadastrada")
    public Pessoa criar(@RequestBody PessoaInput input) {
        return pessoaService.criar(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Alterar pessoa (ADMIN)",
            description = "`senha` vazia mantém a atual; `papel` e `ativo` nulos não mudam. "
                    + "O admin não pode tirar o próprio papel de admin nem se desativar.")
    @ApiResponse(responseCode = "200", description = "Pessoa alterada")
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
    public Pessoa atualizar(@Parameter(description = "Id da pessoa", example = "1") @PathVariable Long id,
                            @RequestBody PessoaInput input) {
        return pessoaService.atualizar(buscar(id), input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir pessoa (ADMIN)", description = "Não é possível excluir a si mesmo.")
    @ApiResponse(responseCode = "204", description = "Pessoa excluída")
    @ApiResponse(responseCode = "404", description = "Pessoa não encontrada")
    public void excluir(@Parameter(description = "Id da pessoa", example = "1") @PathVariable Long id) {
        pessoaService.deletar(buscar(id));
    }
}
