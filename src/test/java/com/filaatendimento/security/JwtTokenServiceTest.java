package com.filaatendimento.security;

import com.filaatendimento.access.domain.UserRole;
import com.filaatendimento.common.InvalidAccessTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Token handling with a fixed clock: issue/read round trip, expiry,
 * tampered and foreign-issuer tokens.
 */
class JwtTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T15:00:00Z");
    private static final String TEST_SECRET = "segredo-de-assinatura-com-mais-de-trinta-e-dois-caracteres";
    private static final String TEST_ISSUER = "issuer-de-teste";

    @Test
    @DisplayName("a issued token round-trips back to the same identity")
    void issuedTokenRoundTripsToTheSameIdentity() {
        JwtTokenService service = serviceAt(NOW, 600, TEST_ISSUER);
        AuthenticatedUser identity = new AuthenticatedUser(7L, "Maria Silva", UserRole.ATTENDANT);

        AuthenticatedUser restored = service.readAuthenticatedUser(service.issueToken(identity));

        assertThat(restored.userId()).isEqualTo(7L);
        assertThat(restored.userName()).isEqualTo("Maria Silva");
        assertThat(restored.role()).isEqualTo(UserRole.ATTENDANT);
    }

    @Test
    @DisplayName("an expired token is always rejected, even with a valid signature")
    void expiredTokensAreRejected() {
        JwtTokenService issuingService = serviceAt(NOW, 600, TEST_ISSUER);
        String token = issuingService.issueToken(new AuthenticatedUser(1L, "Admin", UserRole.ADMIN));

        // Same subject and signature, issued "one hour later".
        JwtTokenService consumingService = serviceAt(NOW.plusSeconds(3600), 600, TEST_ISSUER);

        assertThatThrownBy(() -> consumingService.readAuthenticatedUser(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void tokensWithForeignSignatureAreRejected() {
        JwtTokenService issuingService = serviceAt(NOW, 600, TEST_ISSUER);
        String token = issuingService.issueToken(new AuthenticatedUser(1L, "Admin", UserRole.ADMIN));

        String foreignSecret = TEST_SECRET + "-diferente-comprido-o-suficiente";
        JwtTokenService consumingService = serviceAt(NOW, 600, TEST_ISSUER, foreignSecret);

        assertThatThrownBy(() -> consumingService.readAuthenticatedUser(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    @DisplayName("a valid token issued by a foreign issuer is rejected")
    void tokensOfForeignIssuerAreRejected() {
        JwtTokenService issuingService = serviceAt(NOW, 600, "outro-emissor");
        String token = issuingService.issueToken(new AuthenticatedUser(1L, "Admin", UserRole.ADMIN));

        JwtTokenService consumingService = serviceAt(NOW, 600, TEST_ISSUER);

        assertThatThrownBy(() -> consumingService.readAuthenticatedUser(token))
                .isInstanceOf(InvalidAccessTokenException.class);
    }

    @Test
    @DisplayName("a token shorter than 32 characters can never be created")
    void configurationsWithShortSecretAreImpossible() {
        assertThatThrownBy(() -> new JwtProperties("short-secret", 600, TEST_ISSUER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    private JwtTokenService serviceAt(Instant now, int validitySeconds, String issuer) {
        return serviceAt(now, validitySeconds, issuer, TEST_SECRET);
    }

    private JwtTokenService serviceAt(Instant now, int validitySeconds, String issuer, String secret) {
        JwtProperties properties = new JwtProperties(secret, validitySeconds, issuer);
        return new JwtTokenService(properties, Clock.fixed(now, ZoneOffset.UTC));
    }
}
