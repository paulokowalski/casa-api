package com.kowalski.casaapi.crescimento.foto;

import org.springframework.core.io.Resource;

/**
 * Onde ficam os arquivos das fotos. Hoje em disco ({@link DiscoFotoStorage}); num produto pode virar S3/MinIO
 * sem mudar o resto do módulo. As chaves são caminhos relativos, ex.: {@code familia/crianca/2026/uuid-mini.jpg}.
 */
public interface FotoStorage {

    void gravar(String chave, byte[] conteudo);

    /** Lança {@code NaoEncontradoException} se o arquivo não existir. */
    Resource ler(String chave);

    /** Não reclama se já não existir. */
    void apagar(String chave);
}
