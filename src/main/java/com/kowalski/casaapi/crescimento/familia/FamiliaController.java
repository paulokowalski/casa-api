package com.kowalski.casaapi.crescimento.familia;

import com.kowalski.casaapi.crescimento.familia.FamiliaDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/crescimento/familias", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: famílias", description = "Famílias e quem pode ver ou alterar os dados das crianças")
public class FamiliaController {

    private final FamiliaService service;

    @GetMapping
    @Operation(summary = "Famílias do usuário logado", description = "Lista vazia quando o usuário ainda não faz parte de nenhuma.")
    public List<FamiliaResponse> minhas() {
        return service.minhas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar família", description = "O usuário logado entra como responsável.")
    public FamiliaResponse criar(@RequestBody NovaFamilia dados) {
        return service.criar(dados);
    }

    @PutMapping("/{familiaId}/membros")
    @Operation(summary = "Adicionar membro ou trocar o papel", description = "Só responsáveis. LEITOR só vê os dados.")
    public FamiliaResponse salvarMembro(@PathVariable UUID familiaId, @RequestBody NovoMembro dados) {
        return service.salvarMembro(familiaId, dados);
    }

    @DeleteMapping("/{familiaId}/membros/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover membro", description = "Responsáveis removem qualquer membro; qualquer membro pode sair.")
    public void removerMembro(@PathVariable UUID familiaId, @PathVariable long usuarioId) {
        service.removerMembro(familiaId, usuarioId);
    }
}
