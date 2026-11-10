package com.filaatendimento.access.application;

/** Access token handed out after a successful login. */
public record AuthenticationToken(
        String value,
        String typeOfToken
) {
}
