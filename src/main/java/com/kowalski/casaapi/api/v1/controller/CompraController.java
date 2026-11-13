package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraResponse;
import com.kowalski.casaapi.business.MensagemResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.model.Compra;
import com.kowalski.casaapi.business.service.CompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/compras", produces = MediaType.APPLICATION_JSON_VALUE)
public class CompraController {

    private static final String TODOS = "TODOS";

    private final CompraService compraService;
    private final UsuarioLogado usuarioLogado;

    @GetMapping
    public List<Compra> listar() {
        return compraService.buscarTodos();
    }

    @GetMapping("/parcelas/{ano}/{mes}")
    public CompraResponse parcelasDoMes(@PathVariable String ano, @PathVariable String mes,
                                        @RequestParam String pessoa,
                                        @RequestParam(defaultValue = TODOS) String cartao,
                                        @RequestParam(defaultValue = TODOS) String ultimaParcela) {
        return compraService.buscarPorMesENome(ano, mes, usuarioLogado.nomePessoa(pessoa), cartao, ultimaParcela);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemResponse criar(@RequestBody CompraInput input) {
        compraService.salvar(input);
        return new MensagemResponse("Cadastro realizado com sucesso.");
    }

    @PutMapping("/{id}")
    public MensagemResponse editar(@PathVariable UUID id, @RequestBody CompraInput input) {
        compraService.editar(id, input);
        return new MensagemResponse("Compra editada com sucesso.");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        compraService.remover(id);
    }
}
