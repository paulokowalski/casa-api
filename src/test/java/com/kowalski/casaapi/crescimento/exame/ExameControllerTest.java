package com.kowalski.casaapi.crescimento.exame;

import com.kowalski.casaapi.crescimento.exame.ExameDtos.ExameResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExameControllerTest {

    final ExameService service = mock(ExameService.class);
    final ExameController controller = new ExameController(service);
    final UUID crianca = UUID.randomUUID();
    final MockMultipartFile arquivo = new MockMultipartFile("arquivo", "a.pdf", "application/pdf", "%PDF-1.4".getBytes());

    @Test
    void envioDuplicadoAoMesmoTempoDevolveOExameJaGuardado() throws Exception {
        ExameResponse existente = new ExameResponse(UUID.randomUUID(), crianca, "Hemograma", LocalDate.of(2026, 11, 20),
                null, "a.pdf", TipoExame.PDF, 8, Instant.now(), true);
        when(service.enviar(eq(crianca), any(), any(), any())).thenThrow(new DataIntegrityViolationException("uq_exame_crianca_sha"));
        when(service.jaEnviado(eq(crianca), any())).thenReturn(Optional.of(existente));
        assertThat(controller.enviar(crianca, arquivo, "Hemograma", LocalDate.of(2026, 11, 20), null)).isSameAs(existente);
    }

    @Test
    void outraViolacaoContinuaSendoErro() {
        when(service.enviar(eq(crianca), any(), any(), any())).thenThrow(new DataIntegrityViolationException("outra"));
        when(service.jaEnviado(eq(crianca), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> controller.enviar(crianca, arquivo, "X", LocalDate.of(2026, 11, 20), null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
