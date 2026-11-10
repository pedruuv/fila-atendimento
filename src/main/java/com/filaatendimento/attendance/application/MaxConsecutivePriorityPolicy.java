package com.filaatendimento.attendance.application;

import com.filaatendimento.attendance.domain.PriorityPolicy;
import org.springframework.stereotype.Component;

/**
 * Fairness rule against starvation: after the configured number of priority
 * calls in a row, the next call must come from the regular line, even if
 * priority clients are still waiting.
 */
@Component
public class MaxConsecutivePriorityPolicy implements PriorityPolicy {

    private final AttendanceProperties properties;

    public MaxConsecutivePriorityPolicy(AttendanceProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean shouldCallPriorityFirst(long consecutivePriorityCalls) {
        if (properties.maxConsecutivePriority() == 0) {
            // Cap disabled: regular tickets are only called when the priority line is empty.
            return true;
        }
        return consecutivePriorityCalls < properties.maxConsecutivePriority();
    }
}
