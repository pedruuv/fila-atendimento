package com.filaatendimento.access.domain;

/**
 * Narrow password abstraction so domain and application code never depend
 * directly on Spring Security classes.
 */
public interface PasswordHasher {

    /** Returns the secure hash that must be stored, never the plain password. */
    String hashNew(String plainPassword);

    /** True when the informed plain password matches the stored hash. */
    boolean matches(String candidatePlainPassword, String storedHash);
}
