package com.filaatendimento.attendance.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload required to emit a new queue ticket. */
@Schema(description = "Dados para emitir uma nova senha na fila")
public record TicketIssueRequest(
        @Schema(description = "Nome da pessoa associada à senha", example = "Amanda", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O nome do cliente é obrigatório")
        @Size(max = 120, message = "O nome do cliente deve ter no máximo 120 caracteres")
        String clientName,

        @Schema(description = "Indica se a senha entra na fila prioritária", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "A indicação de prioridade é obrigatória")
        Boolean priority
) {
}
