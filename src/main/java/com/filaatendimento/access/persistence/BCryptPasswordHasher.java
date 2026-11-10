package com.filaatendimento.access.persistence;

import com.filaatendimento.access.domain.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter between Spring Security's {@link PasswordEncoder} (BCrypt) and the
 * domain abstraction {@link PasswordHasher}, keeping the domain free of
 * framework types.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    public BCryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hashNew(String plainPassword) {
        return encoder.encode(plainPassword);
    }

    @Override
    public boolean matches(String candidatePlainPassword, String storedHash) {
        return encoder.matches(candidatePlainPassword, storedHash);
    }
}
