package com.kowalski.casaapi.crescimento.diario;

import com.kowalski.casaapi.crescimento.diario.RegistroDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/crescimento/criancas/{criancaId}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: diário", description = "Momentos, consultas e pesagens com fotos, e a linha do tempo")
public class DiarioController {

    private final RegistroService service;

    @GetMapping("/linha-do-tempo")
    @Operation(summary = "Linha do tempo", description = "Registros do diário e medições avulsas, do mais recente ao mais antigo, "
            + "com fotos, idade e ganho de peso. Agrupe por mesDeVida para separar por mês.")
    public LinhaDoTempoResponse linhaDoTempo(@PathVariable UUID criancaId,
                                             @Parameter(description = "Filtra por tipo (PESAGEM inclui as medições avulsas)") @RequestParam(required = false) TipoRegistro tipo,
                                             @Parameter(description = "Só registros com foto") @RequestParam(defaultValue = "false") boolean soComFotos,
                                             @RequestParam(defaultValue = "0") int pagina,
                                             @RequestParam(defaultValue = "20") int tamanho) {
        return service.linhaDoTempo(criancaId, tipo, soComFotos, pagina, tamanho);
    }

    @PostMapping("/registros")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Novo registro", description = "Medidas informadas viram também uma medição (com percentil). Só responsáveis.")
    public RegistroResponse criar(@PathVariable UUID criancaId, @RequestBody RegistroInput dados) {
        return service.criar(criancaId, dados);
    }

    @GetMapping("/registros/{id}")
    @Operation(summary = "Registro do diário")
    public RegistroResponse buscar(@PathVariable UUID criancaId, @PathVariable UUID id) {
        return service.buscar(criancaId, id);
    }

    @PutMapping("/registros/{id}")
    @Operation(summary = "Alterar registro", description = "Só responsáveis. fotoIds null mantém as fotos; fotos tiradas da lista são apagadas.")
    public RegistroResponse atualizar(@PathVariable UUID criancaId, @PathVariable UUID id, @RequestBody RegistroInput dados) {
        return service.atualizar(criancaId, id, dados);
    }

    @DeleteMapping("/registros/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir registro", description = "Apaga também as fotos e as medidas do registro. Só responsáveis.")
    public void excluir(@PathVariable UUID criancaId, @PathVariable UUID id) {
        service.excluir(criancaId, id);
    }
}
