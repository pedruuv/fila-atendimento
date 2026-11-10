package com.filaatendimento.attendance.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fairness configuration of the attendance queue, bound from the
 * {@code attendance} configuration section.
 *
 * @param maxConsecutivePriority maximum number of priority tickets that may be
 *                              called in a row before a regular ticket must be
 *                              called; 0 disables the cap
 */
@ConfigurationProperties(prefix = "attendance")
public record AttendanceProperties(int maxConsecutivePriority) {

    public AttendanceProperties {
        if (maxConsecutivePriority < 0) {
            throw new IllegalStateException("attendance.max-consecutive-priority não pode ser negativo.");
        }
    }
}
