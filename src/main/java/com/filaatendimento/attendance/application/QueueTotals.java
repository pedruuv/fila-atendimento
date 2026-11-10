package com.filaatendimento.attendance.application;

/** Snapshot of how many tickets are standing in each queue line. */
public record QueueTotals(
        long waitingPriority,
        long waitingRegular
) {

    public long waitingTotal() {
        return waitingPriority + waitingRegular;
    }
}
