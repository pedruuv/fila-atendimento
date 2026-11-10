package com.filaatendimento.attendance.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** Result of clearing the queue: nothing is deleted, just cancelled. */
@Schema(description = "Resultado da limpeza da fila")
public record QueueClearingResponse(
        @Schema(description = "Quantidade de senhas que estavam aguardando e foram canceladas", example = "4")
        long cancelledAmount
) {
}
