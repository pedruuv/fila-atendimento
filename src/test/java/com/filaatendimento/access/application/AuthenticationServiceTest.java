package com.filaatendimento.access.application;

import com.filaatendimento.access.domain.PasswordHasher;
import com.filaatendimento.access.domain.User;
import com.filaatendimento.access.persistence.UserRepository;
import com.filaatendimento.common.InvalidCredentialsException;
import com.filaatendimento.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Login rules: both unknown login and wrong password produce the same error. */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String LOGIN = "maria.silva";
    private static final String CORRECT_PASSWORD = "SenhaCorreta123";
    private static final String HASHED_PASSWORD = "{bcrypt}hash-armazenado";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private JwtTokenService tokenService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(userRepository, passwordHasher, tokenService);
    }

    @Test
    @DisplayName("a valid login with correct password returns an issued Bearer token")
    void validLoginIssuesToken() {
        givenUserWithPassword(CORRECT_PASSWORD, HASHED_PASSWORD);
        when(passwordHasher.matches(CORRECT_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(tokenService.issueToken(any())).thenReturn("token-assinado");

        AuthenticationToken token =
                authenticationService.login(LOGIN, CORRECT_PASSWORD);

        assertThat(token.value()).isEqualTo("token-assinado");
        assertThat(token.typeOfToken()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("an unknown login produces exactly the same shallow error as a wrong password")
    void unknownLoginRaisesSameShallowError() {
        when(userRepository.findByLogin("desconhecido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login("desconhecido", "qualquerSenha123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Login ou senha inválidos");

        assertThatThrownBy(() -> authenticationService.login(LOGIN, "SenhaErrada123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Login ou senha inválidos");
    }

    @Test
    @DisplayName("a wrong password never issues a token")
    void wrongPasswordNeverIssuesToken() {
        givenUserWithPassword(CORRECT_PASSWORD, HASHED_PASSWORD);
        when(passwordHasher.matches("SenhaErrada123", HASHED_PASSWORD)).thenReturn(false);
        assertThatThrownBy(() -> authenticationService.login(LOGIN, "SenhaErrada123"))
                .isInstanceOf(InvalidCredentialsException.class);

        org.mockito.Mockito.verifyNoInteractions(tokenService);
    }

    private void givenUserWithPassword(String plainPassword, String hashedPassword) {
        User storedUser = new User("Maria Silva", LOGIN, hashedPassword,
                com.filaatendimento.access.domain.UserRole.USER);
        when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(storedUser));
    }
}
