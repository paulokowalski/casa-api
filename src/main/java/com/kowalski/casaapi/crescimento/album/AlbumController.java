package com.kowalski.casaapi.crescimento.album;

import com.kowalski.casaapi.crescimento.album.AlbumDtos.AlbumInput;
import com.kowalski.casaapi.crescimento.album.AlbumDtos.AlbumResponse;
import com.kowalski.casaapi.crescimento.diario.RegistroDtos.RegistroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/crescimento/criancas/{criancaId}/album", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Crescimento: álbum", description = "Mêsversários, primeiras vezes e momentos especiais")
public class AlbumController {

    private final AlbumService service;

    @GetMapping
    @Operation(summary = "Álbum", description = "Lugares do álbum com a capa, a situação (FEITO, HOJE, PENDENTE, FUTURO) e o aviso do mêsversário.")
    public AlbumResponse album(@PathVariable UUID criancaId) {
        return service.album(criancaId);
    }

    @PutMapping("/{chave}")
    @Operation(summary = "Guardar fotos num lugar do álbum", description = "Cria ou altera o registro do lugar (ex.: nascimento, mes-3, ano-1, "
            + "primeira-sorriso). O registro aparece também na linha do tempo. Só responsáveis.")
    public RegistroResponse salvar(@PathVariable UUID criancaId, @PathVariable String chave, @RequestBody AlbumInput dados) {
        return service.salvar(criancaId, chave, dados);
    }

    @PostMapping("/especiais")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Novo momento especial", description = "Com o nome em albumTitulo (batizado, primeira praia...). Só responsáveis.")
    public RegistroResponse novoEspecial(@PathVariable UUID criancaId, @RequestBody AlbumInput dados) {
        return service.novoEspecial(criancaId, dados);
    }

    @DeleteMapping("/{chave}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Tirar do álbum", description = "Apaga o registro do lugar com as fotos. Só responsáveis.")
    public void excluir(@PathVariable UUID criancaId, @PathVariable String chave) {
        service.excluir(criancaId, chave);
    }
}
