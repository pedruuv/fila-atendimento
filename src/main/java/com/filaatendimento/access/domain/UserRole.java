package com.filaatendimento.access.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Access levels granted to users of the API.
 *
 * <ul>
 *   <li>{@link #USER}: may issue and consult tickets in the queue.</li>
 *   <li>{@link #ATTENDANT}: besides the USER abilities, may call the next ticket,
 *       cancel individual tickets and list recent calls.</li>
 *   <li>{@link #ADMIN}: besides every ATTENDANT ability, may permanently delete
 *       tickets, clear the queue and list registered users.</li>
 * </ul>
 */
public enum UserRole {

    USER,
    ATTENDANT,
    ADMIN;

    /** Spring Security role name, always prefixed with ROLE_. */
    public String asSpringRole() {
        return "ROLE_" + name();
    }

    /** Simple authority used when building the SecurityContext identity. */
    public GrantedAuthority asAuthority() {
        return new SimpleGrantedAuthority(asSpringRole());
    }
}
