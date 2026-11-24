package com.kowalski.casaapi.crescimento.diario;

import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaService;
import com.kowalski.casaapi.crescimento.diario.RegistroDtos.RegistroInput;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.foto.FotoDtos.FotoResponse;
import com.kowalski.casaapi.crescimento.foto.FotoService;
import com.kowalski.casaapi.crescimento.identidade.DiretorioUsuarios;
import com.kowalski.casaapi.crescimento.medida.MedicaoRepository;
import com.kowalski.casaapi.crescimento.medida.MedicaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SalvarRegistroTest {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 22);
    final RegistroRepository registros = mock(RegistroRepository.class);
    final FotoService fotos = mock(FotoService.class);
    final RegistroService service = new RegistroService(registros, mock(CriancaService.class), mock(MedicaoService.class),
            mock(MedicaoRepository.class), fotos, mock(AcessoFamilia.class), mock(DiretorioUsuarios.class),
            Clock.fixed(HOJE.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    final Crianca crianca = new Crianca();

    @BeforeEach
    void crianca() {
        crianca.setId(UUID.randomUUID());
        crianca.setNascimento(HOJE.minusMonths(4));
        when(registros.saveAndFlush(any())).thenAnswer(i -> {
            Registro r = i.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            return r;
        });
    }

    static RegistroInput momento(String texto, String albumChave, List<UUID> fotoIds) {
        return new RegistroInput(HOJE, TipoRegistro.MOMENTO, texto, null, null, albumChave, null, null, null, null, fotoIds);
    }

    Registro existente() {
        Registro r = new Registro();
        r.setId(UUID.randomUUID());
        r.setCriancaId(crianca.getId());
        r.setData(HOJE);
        r.setTipo(TipoRegistro.MOMENTO);
        return r;
    }

    static FotoResponse fotoDoRegistro(UUID id, UUID registroId) {
        return new FotoResponse(id, null, 10, 10, null, null, null, registroId, false, null);
    }

    @Test
    void semFotoIdsNaAlteracaoMantemAsFotos() {
        Registro r = existente();
        UUID f1 = UUID.randomUUID(), f2 = UUID.randomUUID();
        when(fotos.doRegistros(List.of(r.getId())))
                .thenReturn(Map.of(r.getId(), List.of(fotoDoRegistro(f1, r.getId()), fotoDoRegistro(f2, r.getId()))));

        service.salvar(crianca, r, momento("novo texto", null, null));

        verify(fotos).definirFotosDoRegistro(crianca, r.getId(), List.of(f1, f2));
    }

    @Test
    void listaVaziaNaAlteracaoTiraTodasAsFotos() {
        Registro r = existente();
        service.salvar(crianca, r, momento("só texto", null, List.of()));
        verify(fotos).definirFotosDoRegistro(crianca, r.getId(), List.of());
    }

    @Test
    void lugarDoAlbumSemFotoERecusado() {
        assertThatThrownBy(() -> service.salvar(crianca, null, momento(null, "mes-3", List.of())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("pelo menos uma foto");
        verify(registros, never()).saveAndFlush(any());
        verify(fotos, never()).definirFotosDoRegistro(any(), any(), any());
    }

    @Test
    void lugarDoAlbumRepetidoERecusado() {
        Registro outro = existente();
        outro.setAlbumChave("mes-3");
        when(registros.findByCriancaIdAndAlbumChave(crianca.getId(), "mes-3")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> service.salvar(crianca, null, momento(null, "mes-3", List.of(UUID.randomUUID()))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("já tem um registro");
        verify(registros, never()).saveAndFlush(any());
        verify(fotos, never()).definirFotosDoRegistro(any(), any(), any());

        service.salvar(crianca, outro, momento(null, "mes-3", List.of(UUID.randomUUID())));
        verify(fotos).definirFotosDoRegistro(eq(crianca), eq(outro.getId()), anyList());
    }
}
