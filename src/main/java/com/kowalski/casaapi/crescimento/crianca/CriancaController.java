package com.kowalski.casaapi.crescimento.crianca;

import com.kowalski.casaapi.crescimento.crianca.CriancaDtos.*;
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
@RequestMapping(path = "/v1/crescimento", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: crianças", description = "Cadastro das crianças da família")
public class CriancaController {

    private final CriancaService service;

    @GetMapping("/familias/{familiaId}/criancas")
    @Operation(summary = "Crianças da família", description = "Em ordem de nascimento, com a idade de hoje.")
    public List<CriancaResponse> listar(@PathVariable UUID familiaId) {
        return service.listar(familiaId);
    }

    @PostMapping("/familias/{familiaId}/criancas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar criança", description = "Só responsáveis.")
    public CriancaResponse criar(@PathVariable UUID familiaId, @RequestBody CriancaInput dados) {
        return service.criar(familiaId, dados);
    }

    @GetMapping("/criancas/{id}")
    @Operation(summary = "Dados da criança")
    public CriancaResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PutMapping("/criancas/{id}")
    @Operation(summary = "Alterar dados da criança", description = "Só responsáveis. Também define a foto de perfil (fotoId).")
    public CriancaResponse atualizar(@PathVariable UUID id, @RequestBody CriancaInput dados) {
        return service.atualizar(id, dados);
    }

    @DeleteMapping("/criancas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir criança", description = "Apaga também as medidas e as fotos (inclusive os arquivos). Só responsáveis.")
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }
}
