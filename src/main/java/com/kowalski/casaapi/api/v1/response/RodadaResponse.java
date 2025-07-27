package com.kowalski.casaapi.api.v1.response;

import java.time.LocalDateTime;

public record RodadaResponse(int rodada, LocalDateTime dataJogo, String timeCasa, String imagemTimeCasa, int golsTimeCasa, String timeVisitante, String imagemTimeVisitante, int golsTimeVisitante) { }
