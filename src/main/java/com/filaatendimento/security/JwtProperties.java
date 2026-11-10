package com.filaatendimento.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties used to sign and validate access tokens (JWT).
 * Bound from the {@code security.jwt} configuration section.
 *
 * @param secret          signing secret, HMAC-SHA256; must have at least 32 characters
 * @param validitySeconds how long a token stays valid after being issued
 * @param issuer          value stored in the {@code iss} claim and expected when validating
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        int validitySeconds,
        String issuer
) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "security.jwt.secret deve ter pelo menos 32 caracteres. Defina a variável JWT_SECRET.");
        }
        if (validitySeconds <= 0) {
            throw new IllegalStateException("security.jwt.validity-seconds deve ser um número positivo.");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("security.jwt.issuer é obrigatório.");
        }
    }
}
