package com.filaatendimento.security;

import com.filaatendimento.access.domain.UserRole;

/**
 * Identity of the caller, either obtained from a validated access token
 * (JWT) or used as the subject when issuing one.
 *
 * @param userId   unique identifier of the user
 * @param userName display name of the user
 * @param role     access level of the user
 */
public record AuthenticatedUser(
        Long userId,
        String userName,
        UserRole role
) {
}
