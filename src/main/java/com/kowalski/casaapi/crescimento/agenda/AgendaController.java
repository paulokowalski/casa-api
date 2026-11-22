package com.kowalski.casaapi.crescimento.agenda;

import com.kowalski.casaapi.crescimento.agenda.AgendaService.Compromisso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/crescimento/criancas/{criancaId}/agenda", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: agenda", description = "Próximas vacinas, consulta e mêsversário")
public class AgendaController {

    private final AgendaService service;

    @GetMapping
    @Operation(summary = "Agenda", description = "Vacinas atrasadas e dos próximos 60 dias (agrupadas por data), o retorno marcado e o próximo mêsversário.")
    public List<Compromisso> agenda(@PathVariable UUID criancaId) {
        return service.agenda(criancaId);
    }
}
