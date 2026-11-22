package com.kowalski.casaapi.crescimento.foto;

import com.kowalski.casaapi.crescimento.comum.ArmazenamentoIndisponivelException;
import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.comum.SemPermissaoException;
import com.kowalski.casaapi.crescimento.foto.FotoDtos.FotoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/crescimento", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: fotos", description = "Fotos das crianças, guardadas em disco e servidas por links temporários")
public class FotoController {

    /** Caminho que não exige login (a assinatura do link autoriza). Usado na configuração de segurança. */
    public static final String CAMINHO_ARQUIVO = "/v1/crescimento/fotos/*/arquivo";

    private final FotoService service;
    private final Clock relogio;

    @PostMapping(path = "/criancas/{criancaId}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar foto", description = "JPG ou PNG até 25 MB, no campo 'arquivo'. A foto é endireitada, perde os "
            + "metadados (inclusive GPS) e é gravada em 3 tamanhos. A mesma foto enviada de novo não é duplicada (jaExistia=true). Só responsáveis.")
    public FotoResponse enviar(@PathVariable UUID criancaId, @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return service.enviar(criancaId, arquivo.getBytes());
    }

    @GetMapping("/criancas/{criancaId}/fotos")
    @Operation(summary = "Fotos da criança", description = "Mais recentes primeiro, com links temporários.")
    public List<FotoResponse> listar(@PathVariable UUID criancaId) {
        return service.listar(criancaId);
    }

    @GetMapping("/fotos/{id}")
    @Operation(summary = "Dados da foto", description = "Também serve para renovar os links vencidos.")
    public FotoResponse buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @DeleteMapping("/fotos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir foto", description = "Apaga também os arquivos. Só responsáveis.")
    public void excluir(@PathVariable UUID id) {
        service.excluir(id);
    }

    @GetMapping(path = "/fotos/{id}/arquivo", produces = MediaType.IMAGE_JPEG_VALUE)
    @Operation(summary = "Imagem (link assinado)", description = "Não exige login: use as URLs devolvidas nas respostas. "
            + "Link inválido ou vencido: 403 sem corpo (peça os links de novo em GET /fotos/{id}).")
    public ResponseEntity<Resource> arquivo(@PathVariable UUID id, @RequestParam("v") Variante v,
                                            @RequestParam long exp, @RequestParam String sig) {
        Resource r;
        try {
            r = service.arquivo(id, v, exp, sig);
        } catch (SemPermissaoException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NaoEncontradoException e) {
            return ResponseEntity.notFound().build();
        } catch (ArmazenamentoIndisponivelException e) {
            log.error("{}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        long restante = Math.max(0, exp - relogio.instant().getEpochSecond());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(restante)).cachePrivate().immutable())
                .header("Content-Disposition", "inline; filename=\"foto-" + id + "-" + v.name().toLowerCase() + ".jpg\"")
                .body(r);
    }
}
