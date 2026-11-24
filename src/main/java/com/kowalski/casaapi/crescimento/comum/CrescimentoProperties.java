package com.kowalski.casaapi.crescimento.comum;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "crescimento")
public record CrescimentoProperties(String fotosDiretorio, String linkSegredo, Duration linkValidade,
                                    Integer fotoMaxMegapixels) {

    public CrescimentoProperties {
        if (linkValidade == null || linkValidade.isNegative() || linkValidade.isZero()) linkValidade = Duration.ofHours(6);
        if (fotoMaxMegapixels == null || fotoMaxMegapixels < 1) fotoMaxMegapixels = 16;
    }
}
