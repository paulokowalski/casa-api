package com.kowalski.casaapi.crescimento.vacina;

import com.kowalski.casaapi.crescimento.vacina.VacinaDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/crescimento/criancas/{criancaId}/vacinas", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: vacinas", description = "Carteira de vacinação pelo calendário do SUS")
public class VacinaController {

    private final VacinaService service;

    @GetMapping
    @Operation(summary = "Carteira de vacinação", description = "Doses por idade com a situação (aplicada, tomar agora, atrasada, próxima...) e as extras.")
    public VacinasResponse vacinas(@PathVariable UUID criancaId) {
        return service.vacinas(criancaId);
    }

    @PutMapping("/{vacinaId}")
    @Operation(summary = "Registrar dose do calendário", description = "Ex.: penta2, bcg. Sem data, usa hoje. Só responsáveis.")
    public DoseResponse aplicar(@PathVariable UUID criancaId, @PathVariable String vacinaId, @RequestBody(required = false) VacinaInput dados) {
        return service.aplicar(criancaId, vacinaId, dados);
    }

    @PostMapping("/extras")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar vacina fora do calendário", description = "Só responsáveis.")
    public ExtraResponse aplicarExtra(@PathVariable UUID criancaId, @RequestBody VacinaExtraInput dados) {
        return service.aplicarExtra(criancaId, dados);
    }

    @DeleteMapping("/{vacinaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desfazer registro de vacina", description = "Só responsáveis.")
    public void remover(@PathVariable UUID criancaId, @PathVariable String vacinaId) {
        service.remover(criancaId, vacinaId);
    }
}
