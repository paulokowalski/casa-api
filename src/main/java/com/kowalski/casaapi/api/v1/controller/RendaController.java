package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.api.v1.input.RendaInput;
import com.kowalski.casaapi.api.v1.response.RendaResponse;
import com.kowalski.casaapi.business.auth.UsuarioLogado;
import com.kowalski.casaapi.business.service.RendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static com.kowalski.casaapi.util.Competencias.de;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/rendas", produces = MediaType.APPLICATION_JSON_VALUE)
public class RendaController {

    private final RendaService rendaService;
    private final UsuarioLogado usuarioLogado;

    @GetMapping("/{ano}/{mes}")
    public RendaResponse obter(@PathVariable int ano, @PathVariable int mes, @RequestParam Long pessoaId) {
        return rendaService.obter(usuarioLogado.pessoaId(pessoaId), de(ano, mes));
    }

    @PutMapping("/{ano}/{mes}")
    public RendaResponse salvar(@PathVariable int ano, @PathVariable int mes, @RequestParam Long pessoaId,
                                @RequestBody RendaInput input) {
        return rendaService.salvar(usuarioLogado.pessoaId(pessoaId), de(ano, mes), input);
    }
}
