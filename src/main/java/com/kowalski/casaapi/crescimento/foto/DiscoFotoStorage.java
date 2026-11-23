package com.kowalski.casaapi.crescimento.foto;

import com.kowalski.casaapi.crescimento.comum.ArmazenamentoIndisponivelException;
import com.kowalski.casaapi.crescimento.comum.CrescimentoProperties;
import com.kowalski.casaapi.crescimento.comum.NaoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * Fotos numa pasta do disco. A pasta precisa ter o arquivo {@value #SENTINELA}: se ele não estiver lá, o disco
 * provavelmente caiu ou não montou, e gravar ali poria as fotos no lugar errado — então tudo falha com 503.
 */
@Slf4j
@Component
public class DiscoFotoStorage implements FotoStorage {

    public static final String SENTINELA = ".armazenamento-crescimento";
    private static final Pattern CHAVE = Pattern.compile("[a-z0-9][a-z0-9/_-]*\\.(jpg|pdf)");

    private final Path base;

    public DiscoFotoStorage(CrescimentoProperties props) {
        String dir = props.fotosDiretorio() == null || props.fotosDiretorio().isBlank() ? "/dados/crescimento" : props.fotosDiretorio();
        this.base = Path.of(dir).toAbsolutePath().normalize();
        if (!Files.exists(base.resolve(SENTINELA)))
            log.warn("Fotos do módulo crescimento indisponíveis: falta {} (disco montado? arquivo criado?)", base.resolve(SENTINELA));
    }

    @Override
    public void gravar(String chave, byte[] conteudo) {
        Path destino = resolver(chave);
        verificar();
        try {
            Files.createDirectories(destino.getParent());
            Path tmp = Files.createTempFile(destino.getParent(), ".envio-", ".part");
            try {
                Files.write(tmp, conteudo);
                Files.move(tmp, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            throw new ArmazenamentoIndisponivelException("Falha ao gravar a foto " + destino, e);
        }
    }

    @Override
    public Resource ler(String chave) {
        Path p = resolver(chave);
        verificar();
        if (!Files.isRegularFile(p)) throw new NaoEncontradoException("Arquivo da foto não encontrado");
        return new FileSystemResource(p);
    }

    @Override
    public void apagar(String chave) {
        Path p = resolver(chave);
        verificar();
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            throw new ArmazenamentoIndisponivelException("Falha ao apagar a foto " + p, e);
        }
    }

    private void verificar() {
        if (!Files.exists(base.resolve(SENTINELA)))
            throw new ArmazenamentoIndisponivelException("Pasta das fotos sem o arquivo " + SENTINELA + " (disco desmontado?): " + base, null);
    }

    private Path resolver(String chave) {
        if (chave == null || !CHAVE.matcher(chave).matches() || chave.contains(".."))
            throw new IllegalArgumentException("Chave de foto inválida");
        Path p = base.resolve(chave).normalize();
        if (!p.startsWith(base)) throw new IllegalArgumentException("Chave de foto inválida");
        return p;
    }
}
