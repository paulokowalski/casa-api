package com.kowalski.casaapi.passagens;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/passagens", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Passagens", description = "Rotas monitoradas pelo robô de passagens e o histórico de preços. Só ADMIN.")
public class PassagensController {

    private final PassagensService service;
    private final RotasPassagemService rotas;

    @GetMapping
    @Operation(summary = "Rotas e histórico de preços",
            description = "Rotas cadastradas (ativas e pausadas), uma série de preços por trecho e datas "
                    + "(viagens de hoje em diante), menor/maior/média e último preço de todo o histórico, e alertas enviados. "
                    + "Os pontos do gráfico são do período pedido: até 31 dias, um por busca; acima disso (ou 0 = tudo), "
                    + "o menor preço de cada dia. `desatualizado` = robô sem enviar há mais de duas rodadas.")
    @ApiResponse(responseCode = "200", description = "Rotas e histórico")
    @ApiResponse(responseCode = "503", description = "Postgres indisponível ou robô ainda sem dados")
    public PassagensResponse listar(
            @Parameter(description = "Dias de histórico no gráfico (0 = tudo)", example = "30")
            @RequestParam(defaultValue = "30") int dias) {
        return service.listar(dias);
    }

    @PostMapping(path = "/rotas", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Nova rota", description = "O robô percebe em até 30 s e já faz a primeira busca.")
    @ApiResponse(responseCode = "201", description = "Rota criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou nome repetido")
    public Map<String, Object> criarRota(@RequestBody RotaPassagemInput input) {
        return rotas.criar(input);
    }

    @PutMapping(path = "/rotas/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Editar ou pausar uma rota",
            description = "Substitui a configuração (use `ativa: false` para pausar). Renomear leva junto o histórico.")
    @ApiResponse(responseCode = "200", description = "Rota atualizada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou nome repetido")
    @ApiResponse(responseCode = "404", description = "Rota não encontrada")
    public Map<String, Object> atualizarRota(@PathVariable long id, @RequestBody RotaPassagemInput input) {
        return rotas.atualizar(id, input);
    }

    @DeleteMapping("/rotas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover uma rota", description = "O robô para de buscar. Por padrão o histórico de preços fica guardado.")
    @ApiResponse(responseCode = "204", description = "Rota removida")
    @ApiResponse(responseCode = "404", description = "Rota não encontrada")
    public void removerRota(@PathVariable long id,
                            @Parameter(description = "true = apaga também os preços e alertas desta rota")
                            @RequestParam(defaultValue = "false") boolean historico) {
        rotas.remover(id, historico);
    }
}
