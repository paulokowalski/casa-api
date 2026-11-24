package com.kowalski.casaapi.crescimento.comum;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
@EnableConfigurationProperties(CrescimentoProperties.class)
public class CrescimentoConfig {
    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
}
