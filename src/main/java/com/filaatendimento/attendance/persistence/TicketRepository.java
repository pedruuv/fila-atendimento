package com.filaatendimento.attendance.persistence;

import com.filaatendimento.attendance.domain.Ticket;
import com.filaatendimento.attendance.domain.TicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Persistence of tickets. Locked queries guarantee that two attendants
 * calling at the same time never pick the same ticket.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Next waiting ticket of a given line, arrival order, with a row lock on it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Ticket> findFirstByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus status, boolean priority);

    /** Every standing ticket of a given line, in arrival order. */
    List<Ticket> findByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus status, boolean priority);

    /**
     * How many tickets are standing in a given line; the parameter
     * {@code priority} selects which line is being counted.
     */
    long countByStatusAndPriority(TicketStatus status, boolean priority);

    /** How many tickets are standing across both lines. */
    long countByStatus(TicketStatus status);

    /** Most recent calls, newest first, limited by the given page request. */
    List<Ticket> findByStatusOrderByCalledAtDesc(TicketStatus status, Pageable pageRequest);

    /** Latest calls in descending order (newest first). */
    List<Ticket> findByStatusOrderByCalledAtDesc(TicketStatus status);
}
