package com.filaatendimento.attendance.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Fairness rule: after the cap, the regular line must be served. */
class MaxConsecutivePriorityPolicyTest {

    private MaxConsecutivePriorityPolicy policyWithCap(int cap) {
        return new MaxConsecutivePriorityPolicy(new AttendanceProperties(cap));
    }

    @Test
    @DisplayName("within the cap there is still room to call priority tickets")
    void consecutivePriorityCallsUnderCapAllowPriority() {
        MaxConsecutivePriorityPolicy policy = policyWithCap(3);

        assertThat(policy.shouldCallPriorityFirst(0)).isTrue();
        assertThat(policy.shouldCallPriorityFirst(1)).isTrue();
        assertThat(policy.shouldCallPriorityFirst(2)).isTrue();
    }

    @Test
    @DisplayName("after the cap the regular line must be served next")
    void consecutivePriorityCallsReachingCapForceRegularLine() {
        MaxConsecutivePriorityPolicy policy = policyWithCap(3);

        assertThat(policy.shouldCallPriorityFirst(3)).isFalse();
        assertThat(policy.shouldCallPriorityFirst(7)).isFalse();
    }

    @Test
    @DisplayName("cap zero disables the fairness control: priority always first")
    void capZeroMeansPriorityAlwaysFirst() {
        MaxConsecutivePriorityPolicy policy = policyWithCap(0);

        assertThat(policy.shouldCallPriorityFirst(0)).isTrue();
        assertThat(policy.shouldCallPriorityFirst(10)).isTrue();
    }
}
