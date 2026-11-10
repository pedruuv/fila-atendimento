package com.filaatendimento.access.application;

import com.filaatendimento.access.domain.LoginAlreadyInUseException;
import com.filaatendimento.access.domain.PasswordHasher;
import com.filaatendimento.access.domain.User;
import com.filaatendimento.access.domain.UserRole;
import com.filaatendimento.access.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Registration rules: unique login, hashed password, always USER role. */
@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private UserRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new UserRegistrationService(userRepository, passwordHasher);
    }

    @Test
    @DisplayName("a brand new login is registered with a hashed password and the USER role")
    void registersNewUserWithHashedPassword() {
        when(userRepository.existsByLogin("novo.usuario")).thenReturn(false);
        when(passwordHasher.hashNew("SenhaNova123")).thenReturn("{bcrypt}hash-gerado");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User registered = registrationService.register("Novo Usuário", "novo.usuario", "SenhaNova123");

        assertThat(registered.getLogin()).isEqualTo("novo.usuario");
        assertThat(registered.getRole()).isEqualTo(UserRole.USER);
        // The stored hash must not be the plain password.
        assertThat(registered.getHashedPassword()).isEqualTo("{bcrypt}hash-gerado");
    }

    @Test
    @DisplayName("a login already taken is blocked with a conflict error")
    void duplicatedLoginIsBlocked() {
        when(userRepository.existsByLogin("maria.silva")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register("Outra Maria", "maria.silva", "SenhaXyz123"))
                .isInstanceOf(LoginAlreadyInUseException.class)
                .hasMessageContaining("maria.silva");
    }
}
