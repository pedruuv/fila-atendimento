package com.filaatendimento.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Conflict error: something already exists or a business rule does not
 * allow the operation in the current state of the resource. Resolves the HTTP 409 status.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}
