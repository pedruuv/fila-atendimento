package com.filaatendimento.attendance.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Life cycle rules of the Ticket aggregate, enforced by the aggregate itself.
 */
class TicketTest {

    private static final String TICKET_NUMBER = "007";
    private static final String CLIENT_NAME = "Maria Silva";
    private static final Instant ISSUED_AT = Instant.parse("2026-11-10T13:00:00Z");

    @Test
    @DisplayName("a new ticket is issued WAITING, without call or cancellation times")
    void newTicketStartsWaiting() {
        Ticket ticket = Ticket.issueNew(TICKET_NUMBER, CLIENT_NAME, false, ISSUED_AT);

        assertThat(ticket.getNumber()).isEqualTo(TICKET_NUMBER);
        assertThat(ticket.getClientName()).isEqualTo(CLIENT_NAME);
        assertThat(ticket.isPriority()).isFalse();
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.WAITING);
        assertThat(ticket.getIssuedAt()).isEqualTo(ISSUED_AT);
        assertThat(ticket.getCalledAt()).isNull();
        assertThat(ticket.getCancelledAt()).isNull();
        assertThat(ticket.isWaiting()).isTrue();
    }

    @Test
    @DisplayName("calling a waiting ticket moves it to CALLED and records the moment")
    void callingWaitingTicketBecomesCalled() {
        Ticket ticket = Ticket.issueNew(TICKET_NUMBER, CLIENT_NAME, true, ISSUED_AT);
        Instant momentOfCall = Instant.parse("2026-11-10T13:05:00Z");

        ticket.callAt(momentOfCall);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CALLED);
        assertThat(ticket.getCalledAt()).isEqualTo(momentOfCall);
        assertThat(ticket.isWaiting()).isFalse();
    }

    @Test
    @DisplayName("calling an already CALLED ticket is forbidden")
    void callingAlreadyCalledTicketIsForbidden() {
        Ticket ticket = issuedAndCalledTicket();

        assertThatThrownBy(() -> ticket.callAt(Instant.now()))
                .isInstanceOf(InvalidTicketStateException.class)
                .hasMessageContaining("chamar")
                .hasMessageContaining("CALLED");
    }

    @Test
    @DisplayName("cancelling a CALLED ticket records the cancellation (client did not show up)")
    void cancellingCalledTicketIsAllowed() {
        Ticket ticket = issuedAndCalledTicket();
        Instant momentOfCancellation = Instant.parse("2026-11-10T13:10:00Z");

        ticket.cancelAt(momentOfCancellation);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(ticket.getCancelledAt()).isEqualTo(momentOfCancellation);
    }

    @Test
    @DisplayName("cancelling an already CANCELLED ticket is forbidden")
    void cancellingTwiceIsForbidden() {
        Ticket ticket = Ticket.issueNew(TICKET_NUMBER, CLIENT_NAME, false, ISSUED_AT);
        ticket.cancelAt(Instant.parse("2026-11-10T13:20:00Z"));

        assertThatThrownBy(() -> ticket.cancelAt(Instant.now()))
                .isInstanceOf(InvalidTicketStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    @DisplayName("a CALLED or CANCELLED ticket has no waiting position")
    void nonWaitingTicketHasNoWaitingPosition() {
        Ticket calledTicket = issuedAndCalledTicket();
        ReflectionTestUtils.setField(calledTicket, "id", 40L);

        assertThat(calledTicket.waitingPositionIn(List.of(calledTicket))).isNull();
    }

    @Test
    @DisplayName("position helper answers the 1-based position inside the informed waiting line")
    void waitingPositionMatchesIndexPlusOne() {
        Ticket firstWaiting = persistAsIfLoadedFromDatabase(
                Ticket.issueNew("001", "Cliente A", false, ISSUED_AT), 40L);
        Ticket secondWaiting = persistAsIfLoadedFromDatabase(
                Ticket.issueNew("002", "Cliente B", false, ISSUED_AT.plusSeconds(60)), 50L);

        assertThat(firstWaiting.waitingPositionIn(List.of(firstWaiting, secondWaiting))).isEqualTo(1);
        assertThat(secondWaiting.waitingPositionIn(List.of(firstWaiting, secondWaiting))).isEqualTo(2);
        assertThat(secondWaiting.waitingPositionIn(List.of(firstWaiting))).isNull();
    }

    /** Simulates a JPA managed ticket by filling the normally private id. */
    private Ticket persistAsIfLoadedFromDatabase(Ticket ticket, long id) {
        ReflectionTestUtils.setField(ticket, "id", id);
        return ticket;
    }

    private Ticket issuedAndCalledTicket() {
        Ticket ticket = Ticket.issueNew(TICKET_NUMBER, CLIENT_NAME, false, ISSUED_AT);
        ticket.callAt(ISSUED_AT.plusSeconds(60));
        return ticket;
    }
}
