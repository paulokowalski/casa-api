package com.kowalski.casaapi.crescimento.exame;

import com.kowalski.casaapi.crescimento.comum.CrescimentoProperties;
import com.kowalski.casaapi.crescimento.comum.FotoInvalidaException;
import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import com.kowalski.casaapi.crescimento.comum.SemPermissaoException;
import com.kowalski.casaapi.crescimento.crianca.Crianca;
import com.kowalski.casaapi.crescimento.crianca.CriancaRepository;
import com.kowalski.casaapi.crescimento.exame.ExameDtos.ExameInput;
import com.kowalski.casaapi.crescimento.familia.AcessoFamilia;
import com.kowalski.casaapi.crescimento.familia.FamiliaMembro;
import com.kowalski.casaapi.crescimento.familia.FamiliaMembroRepository;
import com.kowalski.casaapi.crescimento.familia.PapelFamilia;
import com.kowalski.casaapi.crescimento.foto.FotoStorage;
import com.kowalski.casaapi.crescimento.foto.ProcessadorImagem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExameServiceTest {

    static final long PAI = 1L, AVO = 2L, ESTRANHO = 9L;
    static final byte[] PDF = "%PDF-1.7\n1 0 obj <<>> endobj\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    static final LocalDate HOJE = LocalDate.of(2026, 11, 23);

    final UUID familia = UUID.randomUUID();
    final FamiliaMembroRepository membros = mock(FamiliaMembroRepository.class);
    final long[] logado = {PAI};
    final AcessoFamilia acesso = new AcessoFamilia(membros, () -> logado[0]);
    final ExameRepository exames = mock(ExameRepository.class);
    final CriancaRepository criancas = mock(CriancaRepository.class);
    final FotoStorage storage = mock(FotoStorage.class);
    final ProcessadorImagem processador = spy(new ProcessadorImagem(
            new CrescimentoProperties(null, "segredo-de-teste-com-mais-de-32-caracteres!", Duration.ofHours(6), 16)));
    final Clock relogio = Clock.fixed(Instant.parse("2026-11-23T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    final ExameService service = new ExameService(exames, criancas, acesso, storage, processador, relogio);
    final Crianca crianca = new Crianca();
    final ExameInput hemograma = new ExameInput(" Hemograma ", HOJE.minusDays(3), "  ");

    @BeforeEach
    void familia() {
        when(membros.findByFamiliaIdAndUsuarioId(familia, PAI)).thenReturn(Optional.of(membro(PAI, PapelFamilia.RESPONSAVEL)));
        when(membros.findByFamiliaIdAndUsuarioId(familia, AVO)).thenReturn(Optional.of(membro(AVO, PapelFamilia.LEITOR)));
        crianca.setId(UUID.randomUUID());
        crianca.setFamiliaId(familia);
        crianca.setNascimento(LocalDate.of(2026, 5, 1));
        when(criancas.findById(crianca.getId())).thenReturn(Optional.of(crianca));
        when(exames.findByCriancaIdAndSha256(any(), anyString())).thenReturn(Optional.empty());
        when(exames.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
    }

    FamiliaMembro membro(long usuario, PapelFamilia papel) {
        FamiliaMembro m = new FamiliaMembro();
        m.setFamiliaId(familia);
        m.setUsuarioId(usuario);
        m.setPapel(papel);
        return m;
    }

    static byte[] png() throws IOException {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    @Test
    void pdfEGuardadoComoVeio() {
        var r = service.enviar(crianca.getId(), PDF, "C:\\fotos\\hemo\"grama.PDF", hemograma);
        assertThat(r.tipo()).isEqualTo(TipoExame.PDF);
        assertThat(r.titulo()).isEqualTo("Hemograma");
        assertThat(r.observacao()).isNull();
        assertThat(r.nomeArquivo()).isEqualTo("hemograma.pdf");
        ArgumentCaptor<String> chave = ArgumentCaptor.forClass(String.class);
        verify(storage).gravar(chave.capture(), eq(PDF));
        assertThat(chave.getValue()).startsWith("exames/" + familia + "/" + crianca.getId() + "/").endsWith(".pdf");
        verifyNoInteractions(processador);
    }

    @Test
    void imagemViraJpegSemMetadados() throws IOException {
        var r = service.enviar(crianca.getId(), png(), "foto.png", hemograma);
        assertThat(r.tipo()).isEqualTo(TipoExame.JPEG);
        assertThat(r.nomeArquivo()).isEqualTo("foto.jpg");
        ArgumentCaptor<byte[]> conteudo = ArgumentCaptor.forClass(byte[].class);
        verify(storage).gravar(endsWith(".jpg"), conteudo.capture());
        assertThat(ExameService.detectar(conteudo.getValue())).isEqualTo(TipoExame.JPEG);
        assertThat(conteudo.getValue()[0] & 0xFF).isEqualTo(0xFF);
        verify(processador).processarOriginal(any());
        verify(processador, never()).processar(any());
    }

    @Test
    void jaEnviadoDevolveOExameDoMesmoArquivo() {
        Exame e = new Exame();
        e.setId(UUID.randomUUID());
        e.setCriancaId(crianca.getId());
        e.setTipo(TipoExame.PDF);
        e.setBytes(10L);
        when(exames.findByCriancaIdAndSha256(crianca.getId(), ExameService.sha256(PDF))).thenReturn(Optional.of(e));
        assertThat(service.jaEnviado(crianca.getId(), PDF)).hasValueSatisfying(r -> {
            assertThat(r.id()).isEqualTo(e.getId());
            assertThat(r.jaExistia()).isTrue();
        });
        logado[0] = ESTRANHO;
        assertThatThrownBy(() -> service.jaEnviado(crianca.getId(), PDF)).isInstanceOf(NaoEncontradoException.class);
    }

    @Test
    void recusaOutrosFormatosPeloConteudo() {
        assertThatThrownBy(() -> service.enviar(crianca.getId(), "<html>".getBytes(), "exame.pdf", hemograma))
                .isInstanceOf(FotoInvalidaException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void validaTituloEData() {
        assertThatThrownBy(() -> service.enviar(crianca.getId(), PDF, "a.pdf", new ExameInput(" ", HOJE, null)))
                .hasMessageContaining("nome ao exame");
        assertThatThrownBy(() -> service.enviar(crianca.getId(), PDF, "a.pdf", new ExameInput("X", HOJE.plusDays(1), null)))
                .hasMessageContaining("futuro");
        assertThatThrownBy(() -> service.enviar(crianca.getId(), PDF, "a.pdf", new ExameInput("X", null, null)))
                .hasMessageContaining("data");
        verifyNoInteractions(storage);
    }

    @Test
    void mesmoArquivoNaoDuplica() {
        Exame e = new Exame();
        e.setId(UUID.randomUUID());
        e.setCriancaId(crianca.getId());
        e.setTipo(TipoExame.PDF);
        e.setBytes(10L);
        when(exames.findByCriancaIdAndSha256(eq(crianca.getId()), anyString())).thenReturn(Optional.of(e));
        assertThat(service.enviar(crianca.getId(), PDF, "a.pdf", hemograma).jaExistia()).isTrue();
        verifyNoInteractions(storage);
    }

    @Test
    void leitorVeMasNaoEnviaEEstranhoNemVe() {
        logado[0] = AVO;
        assertThatThrownBy(() -> service.enviar(crianca.getId(), PDF, "a.pdf", hemograma)).isInstanceOf(SemPermissaoException.class);
        service.listar(crianca.getId());
        logado[0] = ESTRANHO;
        assertThatThrownBy(() -> service.listar(crianca.getId())).isInstanceOf(NaoEncontradoException.class);
        assertThatThrownBy(() -> service.arquivo(crianca.getId(), UUID.randomUUID())).isInstanceOf(NaoEncontradoException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void exameDeOutraCriancaNaoAbre() {
        Exame e = new Exame();
        e.setId(UUID.randomUUID());
        e.setCriancaId(UUID.randomUUID());
        when(exames.findById(e.getId())).thenReturn(Optional.of(e));
        assertThatThrownBy(() -> service.arquivo(crianca.getId(), e.getId())).isInstanceOf(NaoEncontradoException.class);
        verifyNoInteractions(storage);
    }

    @Test
    void falhaNoBancoApagaOArquivo() {
        when(exames.saveAndFlush(any())).thenThrow(new IllegalStateException("banco fora"));
        assertThatThrownBy(() -> service.enviar(crianca.getId(), PDF, "a.pdf", hemograma)).hasMessage("banco fora");
        verify(storage).apagar(startsWith("exames/"));
    }

    @Test
    void nomeDoArquivo() {
        assertThat(ExameService.nomeArquivo(null, TipoExame.PDF)).isEqualTo("exame.pdf");
        assertThat(ExameService.nomeArquivo("../../etc/passwd", TipoExame.PDF)).isEqualTo("passwd.pdf");
        assertThat(ExameService.nomeArquivo("x".repeat(300) + ".pdf", TipoExame.PDF)).hasSize(200);
    }
}
