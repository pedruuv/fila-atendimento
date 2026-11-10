package com.filaatendimento.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Access token (JWT) missing, malformed, signed with a different key,
 * issued by a different issuer, or already expired. Resolves the HTTP 401 status.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException() {
        super("Token de acesso ausente, expirado ou inválido");
    }
}
