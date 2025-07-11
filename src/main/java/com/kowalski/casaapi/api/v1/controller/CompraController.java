package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.CompraInput;
import com.kowalski.casaapi.api.v1.response.CompraResponse;
import com.kowalski.casaapi.business.MensagemResponse;
import com.kowalski.casaapi.business.model.Compra;
import com.kowalski.casaapi.business.service.CompraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/compra", produces = MediaType.APPLICATION_JSON_VALUE)
public class CompraController {

    private final CompraService compraService;

    @GetMapping
    public List<Compra> buscarTodos(){
        return compraService.buscarTodos();
    }

    @GetMapping("/mes-atual")
    public void buscarMesAtual(){
        compraService.enviarEvento();
    }

    @GetMapping("/{ano}/{mes}/{pessoa}/{cartao}/{ultimaParcelaSelecionado}")
    public CompraResponse buscarPorMesENome(
            @PathVariable String ano,
            @PathVariable String mes,
            @PathVariable String pessoa,
            @PathVariable String cartao,
            @PathVariable String ultimaParcelaSelecionado) {
        return compraService.buscarPorMesENome(ano, mes, pessoa, cartao, ultimaParcelaSelecionado);
    }

    @PostMapping
    public MensagemResponse salvar(@Validated @RequestBody CompraInput compraInput) {
        compraService.salvar(compraInput);
        return new MensagemResponse("Cadastro realizado com sucesso.");
    }

    @DeleteMapping("/{id}")
    public void remover(@PathVariable String id) {
        compraService.remover(UUID.fromString(id));
    }

    @PutMapping("/{id}")
    public MensagemResponse editar(@PathVariable String id, @Validated @RequestBody CompraInput compraInput) {
        compraService.editar(UUID.fromString(id), compraInput);
        return new MensagemResponse("Compra editada com sucesso.");
    }
}