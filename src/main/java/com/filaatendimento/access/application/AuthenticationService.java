package com.filaatendimento.access.application;

import com.filaatendimento.access.domain.User;
import com.filaatendimento.access.domain.PasswordHasher;
import com.filaatendimento.access.persistence.UserRepository;
import com.filaatendimento.common.InvalidCredentialsException;
import com.filaatendimento.security.AuthenticatedUser;
import com.filaatendimento.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authenticates a user by login and password, issuing the Bearer token
 * used by every protected route.
 */
@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtTokenService tokenService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            JwtTokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    /**
     * Issues the access token of a validated user. Both a missing login and a
     * wrong password produce the same error, so callers cannot probe existing logins.
     */
    public AuthenticationToken login(String login, String candidatePlainPassword) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.passwordMatches(candidatePlainPassword, passwordHasher)) {
            throw new InvalidCredentialsException();
        }

        AuthenticatedUser identity = new AuthenticatedUser(user.getId(), user.getName(), user.getRole());
        return new AuthenticationToken(tokenService.issueToken(identity), JwtTokenService.TYPE_OF_TOKEN);
    }
}
