package com.kowalski.casaapi.crescimento.album;

import com.kowalski.casaapi.crescimento.album.CatalogoAlbum.Lugar;
import com.kowalski.casaapi.crescimento.album.CatalogoAlbum.TipoLugar;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.diario.Registro;
import com.kowalski.casaapi.crescimento.foto.FotoDtos.FotoResponse;
import com.kowalski.casaapi.crescimento.medida.Medicao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogoAlbumTest {

    static final LocalDate NASC = LocalDate.of(2026, 1, 31);

    @Test
    void mesversariosAteOProximoAniversario() {
        List<String> bebe = CatalogoAlbum.mesversarios(NASC, NASC.plusMonths(3)).stream().map(Lugar::chave).toList();
        assertThat(bebe).startsWith("nascimento", "mes-1").endsWith("mes-11", "ano-1").doesNotContain("mes-18", "ano-2");
        List<String> doisAnos = CatalogoAlbum.mesversarios(NASC, NASC.plusMonths(25)).stream().map(Lugar::chave).toList();
        assertThat(doisAnos).contains("ano-1", "mes-18", "ano-2", "ano-3").doesNotContain("ano-4");
    }

    @Test
    void mesversarioNoFimDoMesCaiNoUltimoDia() {
        assertThat(CatalogoAlbum.lugar("mes-1", null).orElseThrow().data(NASC)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(CatalogoAlbum.lugar("mes-18", null).orElseThrow().data(NASC)).isEqualTo(LocalDate.of(2027, 7, 31));
    }

    @Test
    void chavesValidasEInvalidas() {
        assertThat(CatalogoAlbum.lugar("nascimento", null)).isPresent();
        assertThat(CatalogoAlbum.lugar("mes-12", null)).isEmpty();
        assertThat(CatalogoAlbum.lugar("mes-0", null)).isEmpty();
        assertThat(CatalogoAlbum.lugar("ano-19", null)).isEmpty();
        assertThat(CatalogoAlbum.lugar("primeira-sorriso", null).orElseThrow().titulo()).isEqualTo("Primeiro sorriso");
        assertThat(CatalogoAlbum.lugar("primeira-inventada", null)).isEmpty();
        Lugar esp = CatalogoAlbum.lugar("especial-a1b2c3d4e5", "Batizado").orElseThrow();
        assertThat(esp.tipo()).isEqualTo(TipoLugar.ESPECIAL);
        assertThat(esp.titulo()).isEqualTo("Batizado");
        assertThat(CatalogoAlbum.lugar("especial-../x", "x")).isEmpty();
        assertThat(CatalogoAlbum.rotulo(CatalogoAlbum.lugar("mes-3", null).orElseThrow())).isEqualTo("Mêsversário · 3 meses");
        assertThat(CatalogoAlbum.rotulo(CatalogoAlbum.lugar("nascimento", null).orElseThrow())).isEqualTo("Nascimento");
    }

    Crianca crianca() {
        Crianca c = new Crianca();
        c.setId(UUID.randomUUID());
        c.setNascimento(LocalDate.of(2026, 1, 10));
        return c;
    }

    @Test
    void chamadaDoMesversario() {
        Crianca c = crianca();
        // faltam 3 dias para 3 meses
        AlbumDtos.Chamada ch = AlbumService.chamada(c, LocalDate.of(2026, 4, 7), Map.of(), Map.of());
        assertThat(ch.chave()).isEqualTo("mes-3");
        assertThat(ch.dias()).isEqualTo(3);
        // 2 meses passou há 5 dias sem foto
        ch = AlbumService.chamada(c, LocalDate.of(2026, 3, 15), Map.of(), Map.of());
        assertThat(ch.chave()).isEqualTo("mes-2");
        assertThat(ch.dias()).isEqualTo(-5);
        // com foto, não avisa (e o próximo está longe)
        Registro r = new Registro();
        r.setId(UUID.randomUUID());
        FotoResponse f = new FotoResponse(UUID.randomUUID(), c.getId(), 1, 1, null, null, null, r.getId(), false, null);
        assertThat(AlbumService.chamada(c, LocalDate.of(2026, 3, 15), Map.of("mes-2", r), Map.of(r.getId(), List.of(f)))).isNull();
    }

    @Test
    void pesoDoMesversarioVemDaPesagemRecente() {
        Registro r = new Registro();
        r.setData(LocalDate.of(2026, 4, 10));
        Medicao antiga = new Medicao();
        antiga.setId(UUID.randomUUID());
        antiga.setData(LocalDate.of(2026, 3, 1));
        antiga.setPesoKg(5.0);
        Medicao recente = new Medicao();
        recente.setId(UUID.randomUUID());
        recente.setData(LocalDate.of(2026, 4, 2));
        recente.setPesoKg(6.1);
        assertThat(AlbumService.pesoEm(r, List.of(antiga, recente))).isEqualTo(6.1);
        assertThat(AlbumService.pesoEm(r, List.of(antiga))).isNull();
    }
}
