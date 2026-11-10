package com.filaatendimento.attendance.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Snapshot of the whole queue: every standing ticket per line plus totals. */
@Schema(description = "Estado atual da fila de atendimento")
public record QueueStateResponse(
        @Schema(description = "Senhas aguardando, primeiro as prioritárias e depois as comuns, em ordem de chegada")
        List<TicketResponse> waitingTickets,

        @Schema(description = "Total de senhas aguardando na fila prioritária", example = "2")
        long waitingPriority,

        @Schema(description = "Total de senhas aguardando na fila comum", example = "5")
        long waitingRegular,

        @Schema(description = "Total de senhas aguardando (as duas filas)", example = "7")
        long waitingTotal
) {

    public static QueueStateResponse of(
            List<TicketResponse> waitingTickets, long waitingPriority, long waitingRegular) {
        return new QueueStateResponse(waitingTickets, waitingPriority, waitingRegular,
                waitingPriority + waitingRegular);
    }
}
