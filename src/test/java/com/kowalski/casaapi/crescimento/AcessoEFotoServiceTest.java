package com.kowalski.casaapi.crescimento;

import com.kowalski.casaapi.crescimento.comum.CrescimentoProperties;
import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.comum.SemPermissaoException;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaRepository;
import com.kowalski.casaapi.crescimento.familia.*;
import com.kowalski.casaapi.crescimento.foto.*;
import com.kowalski.casaapi.crescimento.identidade.DiretorioUsuarios;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AcessoEFotoServiceTest {

    static final long PAI = 1L, AVO = 2L, ESTRANHO = 9L;
    final UUID familia = UUID.randomUUID();
    final FamiliaMembroRepository membros = mock(FamiliaMembroRepository.class);
    final long[] logado = {PAI};
    final AcessoFamilia acesso = new AcessoFamilia(membros, () -> logado[0]);

    final FotoRepository fotos = mock(FotoRepository.class);
    final CriancaRepository criancas = mock(CriancaRepository.class);
    final FotoStorage storage = mock(FotoStorage.class);
    final ProcessadorImagem processador = spy(new ProcessadorImagem(FotosTest.props(null)));
    final LinkAssinado links = new LinkAssinado(FotosTest.props(null), Clock.systemUTC());
    final FotoService fotoService = new FotoService(fotos, criancas, acesso, storage, processador, links, Clock.systemUTC());
    final Crianca crianca = new Crianca();

    @BeforeEach
    void familia() {
        when(membros.findByFamiliaIdAndUsuarioId(familia, PAI)).thenReturn(Optional.of(membro(PAI, PapelFamilia.RESPONSAVEL)));
        when(membros.findByFamiliaIdAndUsuarioId(familia, AVO)).thenReturn(Optional.of(membro(AVO, PapelFamilia.LEITOR)));
        crianca.setId(UUID.randomUUID());
        crianca.setFamiliaId(familia);
        when(criancas.findById(crianca.getId())).thenReturn(Optional.of(crianca));
    }

    FamiliaMembro membro(long usuario, PapelFamilia papel) {
        FamiliaMembro m = new FamiliaMembro();
        m.setFamiliaId(familia);
        m.setUsuarioId(usuario);
        m.setPapel(papel);
        return m;
    }

    @Test
    void responsavelLeEEscreveLeitorSoLeEstranhoNemVe() {
        acesso.exigirEscrita(familia);
        logado[0] = AVO;
        acesso.exigirLeitura(familia);
        assertThat(acesso.podeEscrever(familia)).isFalse();
        assertThatThrownBy(() -> acesso.exigirEscrita(familia)).isInstanceOf(SemPermissaoException.class);
        logado[0] = ESTRANHO;
        assertThatThrownBy(() -> acesso.exigirLeitura(familia)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void naoDeixaAFamiliaSemResponsavel() {
        FamiliaRepository familias = mock(FamiliaRepository.class);
        DiretorioUsuarios usuarios = mock(DiretorioUsuarios.class);
        when(membros.countByFamiliaIdAndPapel(familia, PapelFamilia.RESPONSAVEL)).thenReturn(1L);
        FamiliaService service = new FamiliaService(familias, membros, acesso, usuarios);
        assertThatThrownBy(() -> service.removerMembro(familia, PAI)).hasMessageContaining("pelo menos um responsável");
        verify(membros, never()).delete(any());
    }

    @Test
    void leitorNaoEnviaFoto() {
        logado[0] = AVO;
        assertThatThrownBy(() -> fotoService.enviar(crianca.getId(), FotosTest.png(10, 10))).isInstanceOf(SemPermissaoException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void mesmaFotoDuasVezesNaoDuplica() throws IOException {
        byte[] png = FotosTest.png(20, 20);
        Foto existente = new Foto();
        existente.setId(UUID.randomUUID());
        existente.setCriancaId(crianca.getId());
        existente.setLargura(20);
        existente.setAltura(20);
        when(fotos.findByCriancaIdAndSha256(eq(crianca.getId()), anyString())).thenReturn(Optional.of(existente));
        var r = fotoService.enviar(crianca.getId(), png);
        assertThat(r.jaExistia()).isTrue();
        assertThat(r.urls().mini()).startsWith("/v1/crescimento/fotos/" + existente.getId() + "/arquivo?v=MINI&exp=");
        verify(processador, never()).processar(any());
        verifyNoInteractions(storage);
    }

    @Test
    void falhaNoBancoApagaOsArquivosGravados() throws IOException {
        when(fotos.findByCriancaIdAndSha256(any(), anyString())).thenReturn(Optional.empty());
        when(fotos.saveAndFlush(any())).thenThrow(new IllegalStateException("banco fora"));
        assertThatThrownBy(() -> fotoService.enviar(crianca.getId(), FotosTest.png(30, 30))).hasMessage("banco fora");
        verify(storage, times(3)).gravar(startsWith(familia + "/" + crianca.getId() + "/"), any());
        verify(storage, times(3)).apagar(anyString());
    }

    @Test
    void linkAlteradoNaoAbreAFoto() {
        UUID id = UUID.randomUUID();
        var a = links.assinar(id, Variante.MEDIA);
        assertThatThrownBy(() -> fotoService.arquivo(id, Variante.MEDIA, a.exp(), a.sig() + "x")).isInstanceOf(SemPermissaoException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void propriedadesTemPadroes() {
        CrescimentoProperties p = new CrescimentoProperties(null, null, null, null);
        assertThat(p.linkValidade()).isEqualTo(Duration.ofHours(6));
        assertThat(p.fotoMaxMegapixels()).isEqualTo(16);
    }
}
