package com.kowalski.casaapi.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(Jwt jwt, Admin admin) {

    public record Jwt(String secret, Integer expiracaoHoras) {
    }

    public record Admin(String email, String senha) {
    }
}
