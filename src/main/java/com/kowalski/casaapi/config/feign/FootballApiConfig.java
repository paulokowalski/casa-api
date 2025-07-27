package com.kowalski.casaapi.config.feign;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FootballApiConfig {
    
    @Value("${futebol.api.token}")
    private String apiToken;
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            template.header("X-Auth-Token", apiToken);
            template.header("Content-Type", "application/json");
        };
    }
}