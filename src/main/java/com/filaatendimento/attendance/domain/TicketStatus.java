package com.filaatendimento.attendance.domain;

/**
 * Life cycle states of a {@link Ticket}, enforced by the aggregate itself:
 *
 * <pre>
 *   WAITING -> CALLED    (attendant called the ticket)
 *   WAITING -> CANCELLED (cancelled individually or by clearing the queue)
 *   CALLED  -> CANCELLED (client did not show up or gave up after being called)
 * </pre>
 */
public enum TicketStatus {
    WAITING,
    CALLED,
    CANCELLED
}
