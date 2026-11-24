package com.kowalski.casaapi.crescimento.foto;

import org.springframework.core.io.Resource;

public interface FotoStorage {

    void gravar(String chave, byte[] conteudo);

    Resource ler(String chave);

    void apagar(String chave);
}
