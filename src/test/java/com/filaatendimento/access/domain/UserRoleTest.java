package com.filaatendimento.access.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Role conventions used by the security layer. */
class UserRoleTest {

    @Test
    @DisplayName("Spring roles carry the ROLE_ prefix, matching hasRole()/hasAnyRole() checks")
    void springRolePrefix() {
        assertThat(UserRole.ADMIN.asSpringRole()).isEqualTo("ROLE_ADMIN");
        assertThat(UserRole.ATTENDANT.asSpringRole()).isEqualTo("ROLE_ATTENDANT");
        assertThat(UserRole.USER.asSpringRole()).isEqualTo("ROLE_USER");
    }
}
