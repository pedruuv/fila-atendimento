package com.filaatendimento.access.persistence;

import com.filaatendimento.access.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Persistence of users, identified by a unique login. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);
}
