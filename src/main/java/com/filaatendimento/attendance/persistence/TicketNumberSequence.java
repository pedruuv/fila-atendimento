package com.filaatendimento.attendance.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Global sequence of ticket numbers, persisted as a single row (id 1),
 * so monotonic numbers like '001', '002', ... are issued atomically by
 * locking this row inside the emission transaction.
 */
@Entity
@Table(name = "tb_ticket_number_sequence")
public class TicketNumberSequence {

    /** Fixed single-row identifier. */
    public static final long SINGLE_ROW_ID = 1L;

    @Id
    private Long id;

    /** Highest number issued so far. */
    private Long lastIssuedValue;

    protected TicketNumberSequence() {
        // Framework constructor, only for JPA.
    }

    public TicketNumberSequence(long lastIssuedValue) {
        this.id = SINGLE_ROW_ID;
        this.lastIssuedValue = lastIssuedValue;
    }

    public Long getId() {
        return id;
    }

    public long reserveNextNumber() {
        lastIssuedValue = lastIssuedValue + 1;
        return lastIssuedValue;
    }
}
