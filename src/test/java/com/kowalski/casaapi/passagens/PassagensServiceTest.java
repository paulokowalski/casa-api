package com.kowalski.casaapi.passagens;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PassagensServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-21T12:00:00Z");

    @Test
    void recenteNaoEstaDesatualizado() {
        assertFalse(PassagensService.desatualizado(OffsetDateTime.parse("2026-11-21T08:00:00-03:00"), 1.0, AGORA));
    }

    @Test
    void semEnviarHaMaisDeDuasRodadasEstaDesatualizado() {
        assertTrue(PassagensService.desatualizado(OffsetDateTime.parse("2026-11-21T06:00:00-03:00"), 1.0, AGORA));
    }

    @Test
    void semDataEstaDesatualizado() {
        assertTrue(PassagensService.desatualizado(null, 1.0, AGORA));
    }
}
