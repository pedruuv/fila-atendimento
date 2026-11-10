package com.filaatendimento.access.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * A user of the system, allowed to authenticate and call the API.
 * The password is never stored in plain text: only the BCrypt hash is kept.
 */
@Entity
@Table(name = "tb_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String login;

    private String hashedPassword;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    protected User() {
        // Framework constructor, only for JPA.
    }

    public User(String name, String login, String hashedPassword, UserRole role) {
        this.name = Objects.requireNonNull(name);
        this.login = Objects.requireNonNull(login);
        this.hashedPassword = Objects.requireNonNull(hashedPassword);
        this.role = Objects.requireNonNull(role);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLogin() {
        return login;
    }

    public UserRole getRole() {
        return role;
    }

    /** Only the BCrypt hash is ever exposed; the plain password is never stored. */
    public String getHashedPassword() {
        return hashedPassword;
    }

    /** True when the informed plain password matches the stored hash. */
    public boolean passwordMatches(String candidatePlainPassword, PasswordHasher passwordHasher) {
        return passwordHasher.matches(candidatePlainPassword, hashedPassword);
    }
}
