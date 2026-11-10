package com.filaatendimento.attendance.domain;

/**
 * Chooses which queue line the attendant serves next, based on how many
 * priority tickets were called consecutively. Implemented by
 * {@link MaxConsecutivePriorityPolicy}, guaranteeing the regular line never
 * starves while priority clients still get precedence most of the time.
 */
@FunctionalInterface
public interface PriorityPolicy {

    /**
     * @param consecutivePriorityCallsCalled how many priority tickets were called in a row since the last regular call
     * @return true when the next call should come from the priority line
     */
    boolean shouldCallPriorityFirst(long consecutivePriorityCallsCalled);
}
