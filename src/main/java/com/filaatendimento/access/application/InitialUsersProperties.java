package com.filaatendimento.access.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Bootstrap users created on the first boot, bound from the
 * {@code security.initial-users} configuration section. Used to seed the
 * first ADMIN and ATTENDANT accounts; regular users self-register through the API.
 *
 * <p>Any entry without a password is ignored, so environments can disable
 * bootstrap users instead of going to the database.</p>
 */
@ConfigurationProperties(prefix = "security.initial-users")
public record InitialUsersProperties(List<InitialUser> users) {

    /** Single bootstrap user definition. */
    public record InitialUser(String name, String login, String password, String role) {
    }
}
