package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.response.FiltroResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.comum.RecursoNaoEncontradoException;
import com.kowalski.casaapi.business.service.FiltroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/filtros", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Filtros", description = "Opções para os filtros e formulários das telas (pares código/descrição)")
public class FiltroController {

    private final FiltroService filtroService;
    private final UsuarioLogado usuarioLogado;

    private static List<FiltroResponse> lista(List<FiltroResponse> itens) {
        if (itens == null) throw new RecursoNaoEncontradoException("Nenhuma opção encontrada");
        return itens;
    }

    @GetMapping("/anos")
    @Operation(summary = "Anos com parcelas", description = "Do mais recente para o mais antigo.")
    public List<FiltroResponse> anos() {
        return lista(filtroService.buscarAnos());
    }

    @GetMapping("/meses")
    @Operation(summary = "Meses do ano com parcelas", description = "Código com dois dígitos (\"01\") e nome do mês.")
    public List<FiltroResponse> meses(@Parameter(description = "Ano", example = "2026") @RequestParam String ano) {
        return lista(filtroService.buscarMeses(ano));
    }

    @GetMapping("/pessoas")
    @Operation(summary = "Pessoas com parcelas no mês", description = "O usuário comum recebe só a si mesmo.")
    public List<FiltroResponse> pessoas(@Parameter(description = "Ano", example = "2026") @RequestParam String ano,
                                        @Parameter(description = "Mês (1 a 12)", example = "11") @RequestParam String mes) {
        List<FiltroResponse> pessoas = lista(filtroService.buscarPessoas(ano, mes));
        if (usuarioLogado.isAdmin()) return pessoas;
        return pessoas.stream().filter(p -> usuarioLogado.podeAcessarNome(p.codigo())).toList();
    }

    @GetMapping("/cartoes")
    @Operation(summary = "Cartões usados pela pessoa no mês")
    public List<FiltroResponse> cartoes(@Parameter(description = "Ano", example = "2026") @RequestParam String ano,
                                        @Parameter(description = "Mês (1 a 12)", example = "11") @RequestParam String mes,
                                        @Parameter(description = "Nome da pessoa", example = "PAULO") @RequestParam String pessoa) {
        return lista(filtroService.buscarCartoes(ano, mes, usuarioLogado.nomePessoa(pessoa)));
    }

    @GetMapping("/categorias")
    @Operation(summary = "Categorias", description = "Código = id (UUID) da categoria.")
    public List<FiltroResponse> categorias() {
        return lista(filtroService.buscarCategorias());
    }

    @GetMapping("/tipos-lancamento")
    @Operation(summary = "Tipos de lançamento (cartões)",
            description = "Código = id (UUID), usado em `tipoLancamentoId` ao cadastrar compras.")
    public List<FiltroResponse> tiposLancamento() {
        return lista(filtroService.buscarTiposDeLancamentos());
    }
}
