package com.filaatendimento.attendance.application;

import com.filaatendimento.attendance.domain.InvalidTicketStateException;
import com.filaatendimento.attendance.domain.PriorityPolicy;
import com.filaatendimento.attendance.domain.Ticket;
import com.filaatendimento.attendance.domain.TicketStatus;
import com.filaatendimento.attendance.persistence.TicketNumberSequence;
import com.filaatendimento.attendance.persistence.TicketNumberSequenceRepository;
import com.filaatendimento.attendance.persistence.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Queue behaviours with the persistence simulated: the priority policy,
 * the fallback between lines and number emission.
 */
@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-10T14:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketNumberSequenceRepository numberSequenceRepository;

    @Mock
    private PriorityPolicy priorityPolicy;

    private QueueService queueService;

    @BeforeEach
    void setUp() {
        queueService = new QueueService(ticketRepository, numberSequenceRepository, priorityPolicy, FIXED_CLOCK);
    }

    @Test
    @DisplayName("issuing a ticket reserves the next sequence number formatted with leading zeros")
    void issuingTicketReservesAndFormatsTheNextNumber() {
        when(numberSequenceRepository.lockSingleRow())
                .thenReturn(Optional.of(new TicketNumberSequence(6)));
        when(ticketRepository.save(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Ticket issued = queueService.issueTicket("Amanda", true);

        assertThat(issued.getNumber()).isEqualTo("007");
        assertThat(issued.getStatus()).isEqualTo(TicketStatus.WAITING);
        assertThat(issued.getIssuedAt()).isEqualTo(FIXED_NOW);
        assertThat(issued.getClientName()).isEqualTo("Amanda");
        assertThat(issued.isPriority()).isTrue();
    }

    @Test
    @DisplayName("when the policy allows, the first ticket of the priority line is called")
    void callsPriorityLineFirstWhenPolicyAllows() {
        givenPolicyDecision(true);
        Ticket waitingPriorityTicket = persistedTicket("007", true);
        givenFirstWaiting(waitingPriorityTicket);

        Optional<Ticket> result = queueService.callNextTicket();

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TicketStatus.CALLED);
        assertThat(result.get().getCalledAt()).isEqualTo(FIXED_NOW);
        verify(ticketRepository).save(waitingPriorityTicket);
    }

    @Test
    @DisplayName("when the policy says regular, the first ticket of the regular line is called")
    void callsRegularLineWhenPolicyChoosesIt() {
        givenPolicyDecision(false);
        Ticket waitingRegularTicket = persistedTicket("008", false);
        givenFirstWaiting(waitingRegularTicket);

        Optional<Ticket> result = queueService.callNextTicket();

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TicketStatus.CALLED);
        verify(ticketRepository).save(waitingRegularTicket);
    }

    @Test
    @DisplayName("when the chosen line is empty, falls back to the other line")
    void fallsBackToTheOtherLineWhenChosenLineIsEmpty() {
        givenPolicyDecision(true);
        Ticket waitingRegularTicket = persistedTicket("010", false);
        when(ticketRepository.findFirstByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, true))
                .thenReturn(Optional.empty());
        when(ticketRepository.findFirstByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, false))
                .thenReturn(Optional.of(waitingRegularTicket));

        Optional<Ticket> result = queueService.callNextTicket();

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TicketStatus.CALLED);
    }

    @Test
    @DisplayName("when both lines are empty, returns empty without saving anything")
    void returnsEmptyWhenNoTicketIsWaiting() {
        givenPolicyDecision(true);
        when(ticketRepository.findFirstByStatusAndPriorityOrderByIssuedAtAsc(any(TicketStatus.class), anyBoolean()))
                .thenReturn(Optional.empty());

        assertThat(queueService.callNextTicket()).isEmpty();
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    @DisplayName("query by position answers the ticket at the informed 1-based position")
    void findTicketByPositionAnswersTheRightTicket() {
        Ticket first = persistedTicket("001", false);
        Ticket second = persistedTicket("002", false);
        when(ticketRepository.findByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, false))
                .thenReturn(List.of(first, second));

        assertThat(queueService.findTicketInQueuePosition(1, false)).isSameAs(first);
        assertThat(queueService.findTicketInQueuePosition(2, false)).isSameAs(second);
    }

    @Test
    @DisplayName("positions beyond the line size raise not found")
    void positionsBeyondLineSizeRaiseNotFound() {
        when(ticketRepository.findByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, false))
                .thenReturn(List.of(persistedTicket("001", false)));

        assertThatThrownBy(() -> queueService.findTicketInQueuePosition(2, false))
                .isInstanceOf(com.filaatendimento.attendance.domain.TicketNotFoundException.class);
    }

    @Test
    @DisplayName("clearing the queue cancels all standing tickets of both lines")
    void clearingQueueCancelsEveryStandingTicket() {
        Ticket waitingPriority = persistedTicket("003", true);
        Ticket waitingRegular = persistedTicket("004", false);
        givenWaitingTickets(waitingPriority, waitingRegular);

        List<Ticket> cancelled = queueService.clearQueue();

        assertThat(cancelled).hasSize(2);
        assertThat(cancelled).allSatisfy(ticket ->
                assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED));
        verify(ticketRepository).saveAll(any(Iterable.class));
    }

    @Test
    @DisplayName("cancelling a ticket cancelled before raises 409, never a hidden bug")
    void cancellingAlreadyCancelledTicketRaisesConflict() {
        Ticket alreadyCancelled = persistedTicket("005", false);
        alreadyCancelled.cancelAt(FIXED_NOW.minusSeconds(60));
        when(ticketRepository.findById(95L)).thenReturn(Optional.of(alreadyCancelled));

        assertThatThrownBy(() -> queueService.cancelTicket(95L))
                .isInstanceOf(InvalidTicketStateException.class);
    }

    @Test
    @DisplayName("the concurrent priority streak counts only leading priority calls")
    void consecutivePriorityStreakCountsLeadingPriorityCalls() {
        Ticket olderPriorityCall = calledTicket("103", true);
        Ticket newerRegularCall = calledTicket("104", false);
        when(ticketRepository.findByStatusOrderByCalledAtDesc(TicketStatus.CALLED))
                .thenReturn(List.of(calledTicket("105", true), newerRegularCall, olderPriorityCall));
        when(ticketRepository.findFirstByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, true))
                .thenReturn(Optional.empty());
        when(ticketRepository.findFirstByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, false))
                .thenReturn(Optional.empty());

        queueService.callNextTicket();

        // Latest call is priority, but the one before it is regular: streak must be 1.
        verify(priorityPolicy).shouldCallPriorityFirst(1);
    }

    @Test
    @DisplayName("recent calls list is bounded by the requested limit")
    void recentCallsAreBoundedByRequestedLimit() {
        when(ticketRepository.findByStatusOrderByCalledAtDesc(
                any(TicketStatus.class), any(PageRequest.class)))
                .thenReturn(List.of());

        queueService.listRecentCalls(5);

        verify(ticketRepository).findByStatusOrderByCalledAtDesc(
                TicketStatus.CALLED, PageRequest.ofSize(5));
    }

    private void givenPolicyDecision(boolean decided) {
        lenient().when(priorityPolicy.shouldCallPriorityFirst(any(long.class))).thenReturn(decided);
    }

    private void givenFirstWaiting(Ticket ticket) {
        when(ticketRepository.findFirstByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, true))
                .thenReturn(Optional.of(ticket));
        lenient().when(ticketRepository.findFirstByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, false))
                .thenReturn(Optional.empty());
        lenient().when(ticketRepository.findByStatusOrderByCalledAtDesc(TicketStatus.CALLED))
                .thenReturn(List.of());
    }

    private void givenWaitingTickets(Ticket priorityTicket, Ticket regularTicket) {
        lenient().when(ticketRepository.findByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, true))
                .thenReturn(List.of(priorityTicket));
        lenient().when(ticketRepository.findByStatusAndPriorityOrderByIssuedAtAsc(TicketStatus.WAITING, false))
                .thenReturn(List.of(regularTicket));
        lenient().when(ticketRepository.saveAll(any(Iterable.class)))
                .thenAnswer(invocation -> {
                    Iterable<Ticket> argument = invocation.getArgument(0);
                    return java.util.stream.StreamSupport
                            .stream(argument.spliterator(), false)
                            .toList();
                });
    }

    private Ticket persistedTicket(String number, boolean priority) {
        Ticket ticket = Ticket.issueNew(number, "Cliente " + number, priority, FIXED_NOW.minusSeconds(120));
        org.springframework.test.util.ReflectionTestUtils.setField(ticket, "id", Long.parseLong(number) * 10);
        return ticket;
    }

    private Ticket calledTicket(String number, boolean priority) {
        Ticket ticket = persistedTicket(number, priority);
        ticket.callAt(FIXED_NOW.minusSeconds(60));
        return ticket;
    }
}
