package com.kowalski.casaapi.crescimento.ia;

import com.kowalski.casaapi.crescimento.ia.ResumoDtos.PerguntaInput;
import com.kowalski.casaapi.crescimento.ia.ResumoDtos.PerguntaResponse;
import com.kowalski.casaapi.crescimento.ia.ResumoDtos.ResumoResponse;
import com.kowalski.casaapi.crescimento.ia.ResumoDtos.ResumosResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/crescimento/criancas/{criancaId}/resumos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: resumo com IA", description = "Resumo do mês de vida, perguntas para a consulta e perguntas dos pais à IA")
public class ResumoController {

    private final ResumoService service;

    @GetMapping
    @Operation(summary = "Resumos guardados", description = "Do mês mais recente para o mais antigo, e se a IA está disponível.")
    public ResumosResponse listar(@PathVariable UUID criancaId) {
        return service.listar(criancaId);
    }

    @PostMapping("/{mes}")
    @Operation(summary = "Gerar (ou refazer) o resumo de um mês de vida",
            description = "mes = 0 é o primeiro mês. Envia à IA medidas, textos do diário, marcos e vacinas (sem fotos e sem o nome). "
                    + "Leva alguns segundos. Só responsáveis.")
    public ResumoResponse gerar(@PathVariable UUID criancaId, @PathVariable int mes) {
        return service.gerar(criancaId, mes);
    }

    @DeleteMapping("/{mes}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Apagar o resumo de um mês", description = "Só responsáveis.")
    public void excluir(@PathVariable UUID criancaId, @PathVariable int mes) {
        service.excluir(criancaId, mes);
    }

    @PostMapping("/{mes}/perguntas")
    @Operation(summary = "Perguntar à IA sobre o mês",
            description = "Só sobre a criança e seus cuidados (outros assuntos são recusados com 400). Precisa do resumo do mês. "
                    + "Envia à IA os mesmos dados do resumo, o resumo e as últimas perguntas. Até 20 por criança por dia. Só responsáveis.")
    public PerguntaResponse perguntar(@PathVariable UUID criancaId, @PathVariable int mes, @RequestBody PerguntaInput dados) {
        return service.perguntar(criancaId, mes, dados);
    }

    @DeleteMapping("/perguntas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Apagar uma pergunta", description = "Só responsáveis.")
    public void excluirPergunta(@PathVariable UUID criancaId, @PathVariable UUID id) {
        service.excluirPergunta(criancaId, id);
    }
}
