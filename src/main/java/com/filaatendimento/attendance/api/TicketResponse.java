package com.filaatendimento.attendance.api;

import com.filaatendimento.attendance.domain.Ticket;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Full representation of a ticket, including its live queue position
 * whenever it is still standing in a line.
 */
@Schema(description = "Dados de uma senha da fila de atendimento")
public record TicketResponse(
        @Schema(description = "Identificador único da senha", example = "42")
        Long id,

        @Schema(description = "Número da senha exibido ao cliente", example = "007")
        String number,

        @Schema(description = "Nome da pessoa associada à senha", example = "Maria Silva")
        String clientName,

        @Schema(description = "Indica se a senha pertence à fila prioritária", example = "false")
        boolean priority,

        @Schema(description = "Estado atual da senha", example = "WAITING")
        String status,

        @Schema(description = "Momento da emissão da senha", example = "2026-11-10T13:22:31Z")
        Instant issuedAt,

        @Schema(description = "Momento em que a senha foi chamada (quando chamada)", example = "2026-11-10T13:30:00Z")
        Instant calledAt,

        @Schema(description = "Momento do cancelamento (quando cancelada)", example = "2026-11-10T13:40:00Z")
        Instant cancelledAt,

        @Schema(description = "Posição atual na fila (1-based), nula quando a senha não está aguardando", example = "1")
        Integer queuePosition
) {

    public static TicketResponse fromDomain(Ticket ticket, Integer queuePosition) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getNumber(),
                ticket.getClientName(),
                ticket.isPriority(),
                ticket.getStatus().name(),
                ticket.getIssuedAt(),
                ticket.getCalledAt(),
                ticket.getCancelledAt(),
                queuePosition);
    }

    public static TicketResponse withoutPosition(Ticket ticket) {
        return fromDomain(ticket, null);
    }
}
