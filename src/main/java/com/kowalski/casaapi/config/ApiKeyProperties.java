package com.kowalski.casaapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api")
public record ApiKeyProperties(String key) {

    public boolean configurada() {
        return key != null && !key.isBlank();
    }
}
