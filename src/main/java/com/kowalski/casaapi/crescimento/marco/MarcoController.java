package com.kowalski.casaapi.crescimento.marco;

import com.kowalski.casaapi.crescimento.marco.MarcoDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/crescimento/criancas/{criancaId}/marcos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: marcos", description = "Marcos do desenvolvimento da Caderneta da Criança")
public class MarcoController {

    private final MarcoService service;

    @GetMapping
    @Operation(summary = "Marcos por faixa de idade", description = "Com a fase atual (idade corrigida para prematuros), o que já aconteceu, "
            + "o que merece atenção e brincadeiras para estimular.")
    public MarcosResponse marcos(@PathVariable UUID criancaId) {
        return service.marcos(criancaId);
    }

    @PutMapping("/{marcoId}")
    @Operation(summary = "Marcar como atingido", description = "Sem data, usa hoje. Só responsáveis.")
    public MarcoResponse marcar(@PathVariable UUID criancaId, @PathVariable String marcoId, @RequestBody(required = false) MarcoInput dados) {
        return service.marcar(criancaId, marcoId, dados);
    }

    @DeleteMapping("/{marcoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desmarcar", description = "Só responsáveis.")
    public void desmarcar(@PathVariable UUID criancaId, @PathVariable String marcoId) {
        service.desmarcar(criancaId, marcoId);
    }
}
