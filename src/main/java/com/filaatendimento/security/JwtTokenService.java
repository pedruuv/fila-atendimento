package com.filaatendimento.security;

import com.filaatendimento.access.domain.UserRole;
import com.filaatendimento.common.InvalidAccessTokenException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and validates the access tokens (JWT, HMAC-SHA256) that carry
 * the user identity and role. Accepts a {@link Clock} so expiry can be
 * properly tested.
 */
@Component
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    public static final String TYPE_OF_TOKEN = "Bearer";

    private final SecretKey signingKey;
    private final long validitySeconds;
    private final String expectedIssuer;
    private final Clock clock;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.validitySeconds = properties.validitySeconds();
        this.expectedIssuer = properties.issuer();
        this.clock = clock;
    }

    /** Creates a signed token whose {@code sub} claim is the user id. */
    public String issueToken(AuthenticatedUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(validitySeconds);

        return Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .issuer(expectedIssuer)
                .claim("userName", user.userName())
                .claim("role", user.role().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses, validates (signature, issuer, expiry) and converts a token
     * back into the identity it carries. Any problem raises
     * {@link InvalidAccessTokenException}, never leaking the cause to callers.
     */
    public AuthenticatedUser readAuthenticatedUser(String token) {
        try {
            io.jsonwebtoken.Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> java.util.Date.from(clock.instant()))
                    .requireIssuer(expectedIssuer)
                    .clockSkewSeconds(30)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("userName", String.class),
                    UserRole.valueOf(claims.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException exception) {
            logger.warn("Token JWT rejeitado ({}): {}", exception.getClass().getSimpleName(), exception.getMessage());
            throw new InvalidAccessTokenException();
        }
    }
}
