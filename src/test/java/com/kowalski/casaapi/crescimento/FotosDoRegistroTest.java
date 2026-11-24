package com.kowalski.casaapi.crescimento;

import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaRepository;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.foto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FotosDoRegistroTest {

    final FotoRepository fotos = mock(FotoRepository.class);
    final FotoStorage storage = mock(FotoStorage.class);
    final FotoService service = new FotoService(fotos, mock(CriancaRepository.class), mock(AcessoFamilia.class), storage,
            mock(ProcessadorImagem.class), new LinkAssinado(FotosTest.props(null), Clock.systemUTC()), Clock.systemUTC());
    final Crianca crianca = new Crianca();
    final UUID registro = UUID.randomUUID();

    @BeforeEach
    void crianca() {
        crianca.setId(UUID.randomUUID());
        crianca.setFamiliaId(UUID.randomUUID());
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void limpar() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    Foto foto(UUID criancaId, UUID registroId) {
        Foto f = new Foto();
        f.setId(UUID.randomUUID());
        f.setCriancaId(criancaId);
        f.setRegistroId(registroId);
        f.setChave("fam/cri/2026/" + f.getId());
        return f;
    }

    static void confirmarTransacao() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
    }

    @Test
    void fotoTiradaDaListaEApagadaEOArquivoSoSomeDepoisDoCommit() {
        Foto fica = foto(crianca.getId(), registro);
        Foto sai = foto(crianca.getId(), registro);
        when(fotos.findAllById(List.of(fica.getId()))).thenReturn(List.of(fica));
        when(fotos.findByRegistroIdOrderByOrdemAsc(registro)).thenReturn(List.of(fica, sai));

        service.definirFotosDoRegistro(crianca, registro, List.of(fica.getId()));

        verify(fotos).delete(sai);
        verify(fotos, never()).delete(fica);
        verifyNoInteractions(storage);
        confirmarTransacao();
        for (Variante v : Variante.values()) verify(storage).apagar(sai.getChave() + v.sufixo());
        verify(storage, never()).apagar(startsWith(fica.getChave()));
        assertThat(fica.getOrdem()).isEqualTo((short) 0);
    }

    @Test
    void fotoDePerfilTiradaDaListaSoSaiDoRegistro() {
        Foto perfil = foto(crianca.getId(), registro);
        perfil.setOrdem((short) 0);
        crianca.setFotoId(perfil.getId());
        when(fotos.findAllById(List.of())).thenReturn(List.of());
        when(fotos.findByRegistroIdOrderByOrdemAsc(registro)).thenReturn(List.of(perfil));

        service.definirFotosDoRegistro(crianca, registro, List.of());
        confirmarTransacao();

        verify(fotos, never()).delete(any());
        verify(fotos).save(perfil);
        assertThat(perfil.getRegistroId()).isNull();
        assertThat(perfil.getOrdem()).isNull();
        verifyNoInteractions(storage);
    }

    @Test
    void recusaFotoDeOutraCriancaDeOutroRegistroOuInexistente() {
        Foto deOutraCrianca = foto(UUID.randomUUID(), null);
        when(fotos.findAllById(List.of(deOutraCrianca.getId()))).thenReturn(List.of(deOutraCrianca));
        assertThatThrownBy(() -> service.definirFotosDoRegistro(crianca, registro, List.of(deOutraCrianca.getId())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("não encontrada");

        Foto deOutroRegistro = foto(crianca.getId(), UUID.randomUUID());
        when(fotos.findAllById(List.of(deOutroRegistro.getId()))).thenReturn(List.of(deOutroRegistro));
        assertThatThrownBy(() -> service.definirFotosDoRegistro(crianca, registro, List.of(deOutroRegistro.getId())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("outro registro");

        UUID inexistente = UUID.randomUUID();
        when(fotos.findAllById(List.of(inexistente))).thenReturn(List.of());
        assertThatThrownBy(() -> service.definirFotosDoRegistro(crianca, registro, List.of(inexistente)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(fotos, never()).delete(any());
        verify(fotos, never()).saveAll(any());
        verifyNoInteractions(storage);
    }
}
