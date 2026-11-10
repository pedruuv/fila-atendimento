package com.filaatendimento.access.application;

import com.filaatendimento.access.domain.LoginAlreadyInUseException;
import com.filaatendimento.access.domain.PasswordHasher;
import com.filaatendimento.access.domain.User;
import com.filaatendimento.access.domain.UserRole;
import com.filaatendimento.access.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers brand new public users. Self-registration always grants the
 * USER role; ATTENDANT and ADMIN users exist only through the initial
 * bootstrap configuration, so privilege elevation through this route is impossible.
 */
@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public UserRegistrationService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public User register(String name, String login, String plainPassword) {
        if (userRepository.existsByLogin(login)) {
            throw new LoginAlreadyInUseException(login);
        }

        String hashedPassword = passwordHasher.hashNew(plainPassword);
        User registeredUser = new User(name, login, hashedPassword, UserRole.USER);
        return userRepository.save(registeredUser);
    }
}
