package com.kowalski.casaapi.crescimento.comum;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração do módulo ({@code crescimento.*} no application.yaml).
 *
 * @param fotosDiretorio pasta das fotos no armazenamento em disco; precisa ter o arquivo
 *                       {@value com.kowalski.casaapi.crescimento.foto.DiscoFotoStorage#SENTINELA}, que prova que o disco certo está montado
 * @param linkSegredo    chave HMAC dos links das fotos (pelo menos 32 caracteres)
 * @param linkValidade   por quanto tempo um link de foto vale (no mínimo; os links são arredondados para o
 *                       navegador poder guardar a imagem em cache)
 * @param fotoMaxMegapixels fotos maiores são reduzidas na leitura para caber na memória
 */
@ConfigurationProperties(prefix = "crescimento")
public record CrescimentoProperties(String fotosDiretorio, String linkSegredo, Duration linkValidade,
                                    Integer fotoMaxMegapixels) {

    public CrescimentoProperties {
        if (linkValidade == null || linkValidade.isNegative() || linkValidade.isZero()) linkValidade = Duration.ofHours(6);
        if (fotoMaxMegapixels == null || fotoMaxMegapixels < 1) fotoMaxMegapixels = 16;
    }
}
