package com.filaatendimento.attendance.application;

import com.filaatendimento.attendance.domain.PriorityPolicy;
import com.filaatendimento.attendance.domain.Ticket;
import com.filaatendimento.attendance.domain.TicketNotFoundException;
import com.filaatendimento.attendance.domain.TicketStatus;
import com.filaatendimento.attendance.persistence.TicketNumberSequence;
import com.filaatendimento.attendance.persistence.TicketNumberSequenceRepository;
import com.filaatendimento.attendance.persistence.TicketRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Core application service of the attendance queue: issues numbered tickets,
 * decides and performs the next call honoring the priority policy, cancels,
 * deletes (ADMIN) and clears the queue.
 *
 * <p>Every queue read/write goes straight to the database, so multiple
 * instances of the API share the very same state — the reference version
 * kept in-memory lines that silently diverged from the database.</p>
 */
@Service
public class QueueService {

    private static final int MINIMUM_NUMBER_WIDTH = 3;

    private final TicketRepository ticketRepository;
    private final TicketNumberSequenceRepository numberSequenceRepository;
    private final PriorityPolicy priorityPolicy;
    private final Clock clock;

    public QueueService(
            TicketRepository ticketRepository,
            TicketNumberSequenceRepository numberSequenceRepository,
            PriorityPolicy priorityPolicy,
            Clock clock) {
        this.ticketRepository = ticketRepository;
        this.numberSequenceRepository = numberSequenceRepository;
        this.priorityPolicy = priorityPolicy;
        this.clock = clock;
    }

    /** Emits the next numbered ticket, standing at the back of its line. */
    @Transactional
    public Ticket issueTicket(String clientName, boolean priority) {
        TicketNumberSequence sequence = numberSequenceRepository.lockSingleRow()
                .orElseThrow(() -> new IllegalStateException("Sequência de números de senha não inicializada"));
        long nextNumberValue = sequence.reserveNextNumber();

        Ticket ticket = Ticket.issueNew(
                formatNumber(nextNumberValue), clientName, priority, clock.instant());
        return ticketRepository.save(ticket);
    }

    /**
     * Calls the next ticket, choosing the line with the priority policy and
     * falling back to the other line when the chosen one is empty.
     *
     * @return the called ticket, or empty when nobody is waiting in any line
     */
    @Transactional
    public Optional<Ticket> callNextTicket() {
        Instant momentOfCall = clock.instant();
        long consecutivePriorityCalls = countConsecutivePriorityCalls();

        boolean tryPriorityFirst = priorityPolicy.shouldCallPriorityFirst(consecutivePriorityCalls);

        Ticket candidate = firstWaitingLineTicket(tryPriorityFirst)
                .orElseGet(() -> firstWaitingLineTicket(!tryPriorityFirst).orElse(null));
        if (candidate == null) {
            return Optional.empty();
        }

        candidate.callAt(momentOfCall);
        ticketRepository.save(candidate);
        return Optional.of(candidate);
    }

    /** @throws TicketNotFoundException when no ticket has the informed id */
    public Ticket findTicketById(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    /**
     * Picks the ticket standing at a given 1-based position of one queue line.
     *
     * @throws TicketNotFoundException when the position is out of range
     */
    public Ticket findTicketInQueuePosition(int queuePosition, boolean priorityQueue) {
        if (queuePosition < 1) {
            throw TicketNotFoundException.forInvalidQueuePosition(queuePosition, priorityQueue);
        }

        List<Ticket> line = ticketRepository
                .findByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, priorityQueue);
        if (queuePosition > line.size()) {
            throw TicketNotFoundException.atQueuePosition(queuePosition, priorityQueue);
        }
        return line.get(queuePosition - 1);
    }

    /** 1-based position of the ticket inside its line, null when not waiting. */
    public Integer queuePositionOf(Ticket ticket) {
        if (ticket == null || !ticket.isWaiting()) {
            return null;
        }
        List<Ticket> line = ticketRepository
                .findByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, ticket.isPriority());
        return ticket.waitingPositionIn(line);
    }

    /** Cancels a ticket (kept on the DB for audit) while it still allows cancellation. */
    @Transactional
    public Ticket cancelTicket(Long ticketId) {
        Ticket ticket = findTicketById(ticketId);
        ticket.cancelAt(clock.instant());
        return ticketRepository.save(ticket);
    }

    /** Permanently deletes a ticket record. Intended for ADMIN use only. */
    @Transactional
    public void deleteTicket(Long ticketId) {
        Ticket ticket = findTicketById(ticketId);
        ticketRepository.delete(ticket);
    }

    /**
     * Cancels every ticket standing in both lines, keeping their records.
     * Intended for ADMIN use only.
     */
    @Transactional
    public List<Ticket> clearQueue() {
        Instant momentOfClearing = clock.instant();
        var standing = java.util.stream.Stream.concat(
                ticketRepository.findByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, true).stream(),
                ticketRepository.findByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, false).stream())
                .toList();

        standing.forEach(ticket -> ticket.cancelAt(momentOfClearing));
        return ticketRepository.saveAll(standing);
    }

    /** Every standing ticket of a single line, in arrival order. */
    public List<Ticket> listWaitingTickets(boolean priorityQueue) {
        return ticketRepository
                .findByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, priorityQueue);
    }

    /** Number of tickets standing in each line and in total. */
    public QueueTotals totals() {
        return new QueueTotals(
                ticketRepository.countByStatusAndPriority(TicketStatus.WAITING, true),
                ticketRepository.countByStatusAndPriority(TicketStatus.WAITING, false));
    }

    /** Most recent calls, newest first, up to the requested amount. */
    public List<Ticket> listRecentCalls(int amount) {
        return ticketRepository
                .findByStatusOrderByCalledAtDesc(TicketStatus.CALLED, PageRequest.ofSize(amount));
    }

    /** How many priority tickets were called in a row since the last regular call. */
    private long countConsecutivePriorityCalls() {
        List<Ticket> latestCalls =
                ticketRepository.findByStatusOrderByCalledAtDesc(TicketStatus.CALLED);
        long consecutivePriority = 0;
        for (Ticket latestCall : latestCalls) {
            if (!latestCall.isPriority()) {
                break;
            }
            consecutivePriority++;
        }
        return consecutivePriority;
    }

    private Optional<Ticket> firstWaitingLineTicket(boolean priorityQueue) {
        return ticketRepository
                .findFirstByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, priorityQueue);
    }

    private String formatNumber(long numberValue) {
        return String.format("%0" + MINIMUM_NUMBER_WIDTH + "d", numberValue);
    }
}
