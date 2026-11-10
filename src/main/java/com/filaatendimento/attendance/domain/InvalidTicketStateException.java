package com.filaatendimento.attendance.domain;

import com.filaatendimento.common.ConflictException;

/**
 * Business rule violation while moving a {@link Ticket} through its
 * life cycle, e.g. calling an already CALLED ticket. Resolves HTTP 409.
 */
public class InvalidTicketStateException extends ConflictException {

    public InvalidTicketStateException(String ticketNumber, TicketStatus currentState, String attemptedAction) {
        super("Não é possível %s: a senha %s está atualmente no estado %s"
                .formatted(attemptedAction, ticketNumber, currentState));
    }
}
