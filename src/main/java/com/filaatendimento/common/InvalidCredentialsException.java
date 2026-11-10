package com.filaatendimento.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Authentication failure: the informed login does not exist or the password
 * does not match. The message is intentionally shallow (it does not tell
 * which of the two is wrong) so login attempts cannot be probed by an attacker.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Login ou senha inválidos");
    }
}
