package com.kowalski.casaapi.crescimento;

import com.kowalski.casaapi.crescimento.comum.ArmazenamentoIndisponivelException;
import com.kowalski.casaapi.crescimento.comum.CrescimentoProperties;
import com.kowalski.casaapi.crescimento.comum.FotoInvalidaException;
import com.kowalski.casaapi.crescimento.foto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FotosTest {

    static CrescimentoProperties props(Path dir) {
        return new CrescimentoProperties(dir == null ? null : dir.toString(), "segredo-de-teste-com-mais-de-32-caracteres!", Duration.ofHours(6), 16);
    }

    static byte[] png(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 120, 0));
        g.fillRect(0, 0, w / 2, h);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    static BufferedImage ler(byte[] jpeg) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(jpeg));
    }

    @Test
    void geraOsTresTamanhosEmJpegSemTransparencia() throws IOException {
        var r = new ProcessadorImagem(props(null)).processar(png(3000, 2000));
        assertThat(r.largura()).isEqualTo(3000);
        assertThat(r.altura()).isEqualTo(2000);
        BufferedImage orig = ler(r.arquivos().get(Variante.ORIGINAL));
        BufferedImage media = ler(r.arquivos().get(Variante.MEDIA));
        BufferedImage mini = ler(r.arquivos().get(Variante.MINI));
        assertThat(orig.getWidth()).isEqualTo(3000);
        assertThat(media.getWidth()).isEqualTo(1600);
        assertThat(media.getHeight()).isEqualTo(1067);
        assertThat(mini.getWidth()).isEqualTo(480);
        assertThat(mini.getHeight()).isEqualTo(320);
        // parte transparente vira branca
        assertThat(new Color(mini.getRGB(470, 10)).getRed()).isGreaterThan(240);
        // JPEG gravado do zero: sem bloco EXIF (nem GPS)
        assertThat(new String(r.arquivos().get(Variante.ORIGINAL), StandardCharsets.ISO_8859_1)).doesNotContain("Exif");
    }

    @Test
    void fotoGrandeDemaisEReduzidaNaLeitura() throws IOException {
        CrescimentoProperties p = new CrescimentoProperties(null, null, null, 1);
        var r = new ProcessadorImagem(p).processar(png(2000, 1500));
        assertThat((long) r.largura() * r.altura()).isLessThanOrEqualTo(1_000_000);
    }

    @Test
    void recusaArquivoQueNaoEImagem() {
        ProcessadorImagem p = new ProcessadorImagem(props(null));
        assertThatThrownBy(() -> p.processar("não é foto".getBytes(StandardCharsets.UTF_8))).isInstanceOf(FotoInvalidaException.class);
        assertThatThrownBy(() -> p.processar(new byte[0])).isInstanceOf(FotoInvalidaException.class);
    }

    @Test
    void linkAssinadoValeAteVencerENaoAceitaAlteracao() {
        Instant agora = Instant.parse("2026-11-16T12:00:00Z");
        LinkAssinado links = new LinkAssinado(props(null), Clock.fixed(agora, ZoneOffset.UTC));
        UUID id = UUID.randomUUID();
        LinkAssinado.Assinatura a = links.assinar(id, Variante.MINI);
        assertThat(a.exp() - agora.getEpochSecond()).isBetween(6 * 3600L, 12 * 3600L);
        assertThat(links.assinar(id, Variante.MINI)).isEqualTo(a); // mesmo link: cache do navegador
        assertThat(links.valido(id, Variante.MINI, a.exp(), a.sig())).isTrue();
        assertThat(links.valido(id, Variante.ORIGINAL, a.exp(), a.sig())).isFalse();
        assertThat(links.valido(UUID.randomUUID(), Variante.MINI, a.exp(), a.sig())).isFalse();
        assertThat(links.valido(id, Variante.MINI, a.exp() + 3600, a.sig())).isFalse();
        assertThat(links.valido(id, Variante.MINI, a.exp(), null)).isFalse();
        LinkAssinado depois = new LinkAssinado(props(null), Clock.fixed(Instant.ofEpochSecond(a.exp() + 1), ZoneOffset.UTC));
        assertThat(depois.valido(id, Variante.MINI, a.exp(), a.sig())).isFalse();
    }

    @Test
    void discoGravaLeEApaga(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve(DiscoFotoStorage.SENTINELA));
        DiscoFotoStorage s = new DiscoFotoStorage(props(dir));
        String chave = "fam/cri/2026/" + UUID.randomUUID() + "-mini.jpg";
        s.gravar(chave, new byte[]{1, 2, 3});
        assertThat(s.ler(chave).getContentAsByteArray()).containsExactly(1, 2, 3);
        try (var arquivos = Files.walk(dir)) {
            assertThat(arquivos.filter(p -> p.getFileName().toString().endsWith(".part"))).isEmpty();
        }
        s.apagar(chave);
        s.apagar(chave);
        assertThat(Files.exists(dir.resolve(chave))).isFalse();
    }

    @Test
    void discoSemArquivoDeControleRecusaGravar(@TempDir Path dir) {
        DiscoFotoStorage s = new DiscoFotoStorage(props(dir));
        assertThatThrownBy(() -> s.gravar("a/b-mini.jpg", new byte[]{1})).isInstanceOf(ArmazenamentoIndisponivelException.class);
        assertThat(dir.resolve("a")).doesNotExist();
    }

    @Test
    void discoNaoSaiDaPasta(@TempDir Path dir) throws IOException {
        Files.createFile(dir.resolve(DiscoFotoStorage.SENTINELA));
        DiscoFotoStorage s = new DiscoFotoStorage(props(dir));
        assertThatThrownBy(() -> s.gravar("../fora-mini.jpg", new byte[]{1})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.ler("/etc/passwd")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> s.ler("a/../../x.jpg")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void orientacaoExifGiraOsPixels() {
        BufferedImage img = new BufferedImage(4, 2, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, Color.RED.getRGB());
        BufferedImage girada = invocarOrientar(img, 6);
        assertThat(girada.getWidth()).isEqualTo(2);
        assertThat(girada.getHeight()).isEqualTo(4);
        assertThat(new Color(girada.getRGB(1, 0))).isEqualTo(Color.RED);
        BufferedImage r180 = invocarOrientar(img, 3);
        assertThat(new Color(r180.getRGB(3, 1))).isEqualTo(Color.RED);
        assertThat(invocarOrientar(img, 1)).isSameAs(img);
    }

    private static BufferedImage invocarOrientar(BufferedImage img, int o) {
        try {
            var m = ProcessadorImagem.class.getDeclaredMethod("orientar", BufferedImage.class, int.class);
            m.setAccessible(true);
            return (BufferedImage) m.invoke(null, img, o);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
