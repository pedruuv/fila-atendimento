package com.filaatendimento.attendance.domain;

import com.filaatendimento.common.NotFoundException;

/** Throws 404 when no ticket matches an informed identifier or queue position. */
public class TicketNotFoundException extends NotFoundException {

    public TicketNotFoundException(Long ticketId) {
        super("Senha com id %d não encontrada".formatted(ticketId));
    }

    public static TicketNotFoundException atQueuePosition(int queuePosition, boolean priorityQueue) {
        String queueName = priorityQueue ? "prioritária" : "comum";
        return new TicketNotFoundException(
                "Não existe senha aguardando na posição %d da fila %s".formatted(queuePosition, queueName));
    }

    public static TicketNotFoundException forInvalidQueuePosition(int queuePosition, boolean priorityQueue) {
        return new TicketNotFoundException(
                "A posição %d é inválida: informe uma posição maior que zero".formatted(queuePosition));
    }

    private TicketNotFoundException(String message) {
        super(message);
    }
}
