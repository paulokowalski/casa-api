package com.kowalski.casaapi.crescimento.foto;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.kowalski.casaapi.crescimento.comum.CrescimentoProperties;
import com.kowalski.casaapi.crescimento.comum.FotoInvalidaException;
import org.springframework.stereotype.Component;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Lê a foto enviada, endireita pela orientação do EXIF e grava as variantes em JPEG.
 * A regravação descarta todos os metadados (inclusive a localização GPS). Uma foto por vez, para caber na memória.
 */
@Component
public class ProcessadorImagem {

    private static final long MAX_PIXELS_ARQUIVO = 250_000_000L;
    private static final DateTimeFormatter EXIF_DATA = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    private final Semaphore umaPorVez = new Semaphore(1, true);
    private final long maxPixels;

    public ProcessadorImagem(CrescimentoProperties props) {
        this.maxPixels = props.fotoMaxMegapixels() * 1_000_000L;
    }

    public record ImagemProcessada(Map<Variante, byte[]> arquivos, int largura, int altura, LocalDateTime tiradaEm) {
    }

    public ImagemProcessada processar(byte[] dados) {
        if (dados == null || dados.length == 0) throw new FotoInvalidaException("O arquivo está vazio");
        try {
            umaPorVez.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento interrompido", e);
        }
        try {
            Exif exif = lerExif(dados);
            BufferedImage img = orientar(ler(dados), exif.orientacao());
            Map<Variante, byte[]> arquivos = new EnumMap<>(Variante.class);
            BufferedImage atual = img;
            for (Variante v : Variante.values()) { // em ordem decrescente de tamanho: cada uma reduz a anterior
                if (v.maiorLado() > 0) atual = reduzir(atual, v.maiorLado());
                arquivos.put(v, jpeg(atual, v.qualidade()));
            }
            return new ImagemProcessada(arquivos, img.getWidth(), img.getHeight(), exif.tiradaEm());
        } catch (IOException e) {
            throw new FotoInvalidaException("Não consegui ler a imagem. Envie uma foto em JPG ou PNG.");
        } finally {
            umaPorVez.release();
        }
    }

    private BufferedImage ler(byte[] dados) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(dados))) {
            Iterator<ImageReader> leitores = in == null ? null : ImageIO.getImageReaders(in);
            if (leitores == null || !leitores.hasNext())
                throw new FotoInvalidaException("Formato não suportado. Envie JPG ou PNG (fotos HEIC do iPhone são convertidas pelo aplicativo antes do envio).");
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(in, true, true);
                long pixels = (long) leitor.getWidth(0) * leitor.getHeight(0);
                if (pixels > MAX_PIXELS_ARQUIVO) throw new FotoInvalidaException("A imagem tem resolução grande demais");
                ImageReadParam param = leitor.getDefaultReadParam();
                int sub = (int) Math.ceil(Math.sqrt((double) pixels / maxPixels));
                if (sub > 1) param.setSourceSubsampling(sub, sub, 0, 0);
                BufferedImage img = leitor.read(0, param);
                return semTransparencia(img);
            } finally {
                leitor.dispose();
            }
        }
    }

    /** JPEG não tem canal alfa: fundo branco. Imagens já em RGB passam direto. */
    private static BufferedImage semTransparencia(BufferedImage img) {
        int t = img.getType();
        if (t == BufferedImage.TYPE_3BYTE_BGR || t == BufferedImage.TYPE_INT_RGB) return img;
        BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return rgb;
    }

    /** Aplica a orientação EXIF (1 a 8) aos pixels. */
    static BufferedImage orientar(BufferedImage img, int orientacao) {
        int w = img.getWidth(), h = img.getHeight();
        AffineTransform t = switch (orientacao) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, w, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, w, h);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, h);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, h, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, h, w);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, w);
            default -> null;
        };
        if (t == null) return img;
        boolean gira = orientacao >= 5;
        BufferedImage destino = new BufferedImage(gira ? h : w, gira ? w : h, img.getType());
        Graphics2D g = destino.createGraphics();
        g.drawImage(img, t, null);
        g.dispose();
        return destino;
    }

    /** Reduz para o maior lado caber em {@code maiorLado}, em etapas de metade (melhor qualidade). */
    static BufferedImage reduzir(BufferedImage img, int maiorLado) {
        int w = img.getWidth(), h = img.getHeight();
        if (Math.max(w, h) <= maiorLado) return img;
        double escala = (double) maiorLado / Math.max(w, h);
        int alvoW = Math.max(1, (int) Math.round(w * escala)), alvoH = Math.max(1, (int) Math.round(h * escala));
        BufferedImage atual = img;
        while (atual.getWidth() / 2 >= alvoW && atual.getHeight() / 2 >= alvoH)
            atual = desenhar(atual, atual.getWidth() / 2, atual.getHeight() / 2);
        return atual.getWidth() == alvoW && atual.getHeight() == alvoH ? atual : desenhar(atual, alvoW, alvoH);
    }

    private static BufferedImage desenhar(BufferedImage img, int w, int h) {
        BufferedImage r = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = r.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return r;
    }

    private static byte[] jpeg(BufferedImage img, float qualidade) throws IOException {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(saida)) {
            escritor.setOutput(out);
            ImageWriteParam p = escritor.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(qualidade);
            p.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            escritor.write(null, new IIOImage(img, null, null), p);
        } finally {
            escritor.dispose();
        }
        return saida.toByteArray();
    }

    record Exif(int orientacao, LocalDateTime tiradaEm) {
    }

    /** Orientação e data em que a foto foi tirada; EXIF ausente ou estragado não impede o envio. */
    static Exif lerExif(byte[] dados) {
        int orientacao = 1;
        LocalDateTime data = null;
        try {
            Metadata md = ImageMetadataReader.readMetadata(new ByteArrayInputStream(dados));
            ExifIFD0Directory ifd0 = md.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null && ifd0.containsTag(ExifIFD0Directory.TAG_ORIENTATION))
                orientacao = ifd0.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            ExifSubIFDDirectory sub = md.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            String txt = sub != null ? sub.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL) : null;
            if (txt == null && ifd0 != null) txt = ifd0.getString(ExifIFD0Directory.TAG_DATETIME);
            if (txt != null) data = LocalDateTime.parse(txt.trim(), EXIF_DATA);
        } catch (Exception e) {
            // sem EXIF legível
        }
        return new Exif(orientacao >= 1 && orientacao <= 8 ? orientacao : 1, data);
    }
}
