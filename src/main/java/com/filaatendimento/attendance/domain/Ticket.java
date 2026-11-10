package com.filaatendimento.attendance.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root of the attendance queue: a numbered token given to a client
 * that models their place in the line.
 *
 * <p>All state transitions are validated here, making it impossible for
 * any service to move a ticket through an illegal path.</p>
 *
 * <p>Tickets are stored as immutable facts of the line: cancelling or being
 * called updates the state but never removes the record, preserving the
 * audit trail (who was called, when, and what was cancelled).</p>
 */
@Entity
@Table(name = "tb_ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human readable number, e.g. '001', printed/displayed for the client. */
    private String number;

    private String clientName;

    /** True when the ticket belongs to the priority line. */
    private boolean priority;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    private Instant issuedAt;

    private Instant calledAt;

    private Instant cancelledAt;

    protected Ticket() {
        // Framework constructor, only for JPA.
    }

    /**
     * Creates a brand new ticket standing in the WAITING state.
     *
     * @param number  human readable number of the ticket, unique, e.g. '001'
     * @param clientName display name of the client holding the ticket
     * @param priority true when joining the priority line
     * @param issuedAt moment of emission
     */
    public static Ticket issueNew(String number, String clientName, boolean priority, Instant issuedAt) {
        Ticket ticket = new Ticket();
        ticket.number = Objects.requireNonNull(number);
        ticket.clientName = Objects.requireNonNull(clientName);
        ticket.priority = priority;
        ticket.status = TicketStatus.WAITING;
        ticket.issuedAt = Objects.requireNonNull(issuedAt);
        return ticket;
    }

    /**
     * Moves the ticket from WAITING to CALLED.
     * @throws InvalidTicketStateException when the ticket is not WAITING
     */
    public void callAt(Instant momentOfCall) {
        Objects.requireNonNull(momentOfCall);
        if (status != TicketStatus.WAITING) {
            throw new InvalidTicketStateException(number, status, "chamar");
        }
        status = TicketStatus.CALLED;
        calledAt = momentOfCall;
    }

    /**
     * Moves the ticket to CANCELLED, keeping the record for audit purposes.
     * @throws InvalidTicketStateException when the ticket is already CANCELLED
     */
    public void cancelAt(Instant momentOfCancellation) {
        Objects.requireNonNull(momentOfCancellation);
        if (status == TicketStatus.CANCELLED) {
            throw new InvalidTicketStateException(number, status, "cancelar");
        }
        status = TicketStatus.CANCELLED;
        cancelledAt = momentOfCancellation;
    }

    /** True while the ticket still stands in one of the queue lines. */
    public boolean isWaiting() {
        return status == TicketStatus.WAITING;
    }

    /** 1-based position in the waiting line it belongs to, null when not waiting. */
    public Integer waitingPositionIn(List<Ticket> waitingTicketsOfSameLine) {
        if (!isWaiting()) {
            return null;
        }
        for (int positionIndex = 0; positionIndex < waitingTicketsOfSameLine.size(); positionIndex++) {
            Ticket candidate = waitingTicketsOfSameLine.get(positionIndex);
            if (candidate.getId() != null && candidate.getId().equals(this.id)) {
                return positionIndex + 1;
            }
        }
        return null;
    }

    public Long getId() { return id; }
    public String getNumber() { return number; }
    public String getClientName() { return clientName; }
    public boolean isPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getCalledAt() { return calledAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}
