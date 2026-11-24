package com.kowalski.casaapi.crescimento.exame;

import com.kowalski.casaapi.crescimento.exame.ExameDtos.ArquivoExame;
import com.kowalski.casaapi.crescimento.exame.ExameDtos.ExameInput;
import com.kowalski.casaapi.crescimento.exame.ExameDtos.ExameResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/crescimento/criancas/{criancaId}/exames", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: exames", description = "Exames da criança (PDF ou imagem), vistos só pela família e com login")
public class ExameController {

    private final ExameService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar exame", description = "PDF, JPG ou PNG até 25 MB no campo 'arquivo', com 'titulo', 'data' "
            + "(AAAA-MM-DD) e 'observacao' opcional. Imagens perdem os metadados (inclusive GPS). O mesmo arquivo enviado de "
            + "novo não é duplicado (jaExistia=true). Só responsáveis.")
    public ExameResponse enviar(@PathVariable UUID criancaId, @RequestParam("arquivo") MultipartFile arquivo,
                                @RequestParam String titulo,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                @RequestParam(required = false) String observacao) throws IOException {
        byte[] dados = arquivo.getBytes();
        try {
            return service.enviar(criancaId, dados, arquivo.getOriginalFilename(), new ExameInput(titulo, data, observacao));
        } catch (DataIntegrityViolationException e) {
            return service.jaEnviado(criancaId, dados).orElseThrow(() -> e);
        }
    }

    @GetMapping
    @Operation(summary = "Exames da criança", description = "Do mais recente para o mais antigo (pela data do exame).")
    public List<ExameResponse> listar(@PathVariable UUID criancaId) {
        return service.listar(criancaId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar nome, data e observação do exame", description = "Só responsáveis.")
    public ExameResponse atualizar(@PathVariable UUID criancaId, @PathVariable UUID id, @RequestBody ExameInput dados) {
        return service.atualizar(criancaId, id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir exame", description = "Apaga também o arquivo. Só responsáveis.")
    public void excluir(@PathVariable UUID criancaId, @PathVariable UUID id) {
        service.excluir(criancaId, id);
    }

    @GetMapping(path = "/{id}/arquivo", produces = {MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_JPEG_VALUE})
    @Operation(summary = "Arquivo do exame", description = "Exige login e ser da família. Não fica em cache.")
    public ResponseEntity<Resource> arquivo(@PathVariable UUID criancaId, @PathVariable UUID id) {
        ArquivoExame a = service.arquivo(criancaId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.tipo().contentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Robots-Tag", "noindex, nofollow, noarchive, noai, noimageai")
                .header("Content-Disposition", ContentDisposition.inline().filename(a.nomeArquivo(), StandardCharsets.UTF_8).build().toString())
                .body(a.conteudo());
    }
}
