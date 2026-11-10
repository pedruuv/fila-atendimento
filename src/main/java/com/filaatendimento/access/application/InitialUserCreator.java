package com.filaatendimento.access.application;

import com.filaatendimento.access.domain.PasswordHasher;
import com.filaatendimento.access.domain.User;
import com.filaatendimento.access.domain.UserRole;
import com.filaatendimento.access.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the bootstrap users declared in {@code security.initial-users}
 * (as ADMIN and ATTENDANT accounts) on the first boot, skipping logins that
 * already exist. Should run before any traffic reaches the API.
 */
@Transactional
@Component
public class InitialUserCreator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(InitialUserCreator.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final InitialUsersProperties initialUsersProperties;

    public InitialUserCreator(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            InitialUsersProperties initialUsersProperties) {
        this.userRepository = userRepository;
        this.initialUsersProperties = initialUsersProperties;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (initialUsersProperties.users() == null) {
            return;
        }

        for (InitialUsersProperties.InitialUser definition : initialUsersProperties.users()) {
            if (definition.login() == null || definition.login().isBlank()) {
                continue;
            }
            if (definition.password() == null || definition.password().isBlank()) {
                logger.info("Usuário inicial {} ignorado: senha não configurada", definition.login());
                continue;
            }
            if (userRepository.existsByLogin(definition.login())) {
                continue;
            }

            UserRole bootstrapRole = UserRole.valueOf(definition.role());
            User createdUser = new User(
                    definition.name(),
                    definition.login(),
                    passwordHasher.hashNew(definition.password()),
                    bootstrapRole);
            userRepository.save(createdUser);
            logger.info(
                    "Usuário inicial criado: {} (papel {})", definition.login(), bootstrapRole);
        }
    }
}
