package com.kowalski.casaapi.servidor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Um evento do servidor: login SSH, backup, alerta, queda de internet, disco, container, kernel ou boot")
public record ServidorEventoResponse(
        @Schema(description = "Quando aconteceu", example = "2026-11-15T03:41:00") LocalDateTime quando,
        @Schema(description = "ssh, backup, alerta, servico, disco, container, internet, tailscale, reboot, kernel ou boot", example = "backup") String tipo,
        @Schema(description = "info, sucesso, atencao ou erro", example = "sucesso") String nivel,
        @Schema(example = "Backup concluído em 6 min") String mensagem) { }
