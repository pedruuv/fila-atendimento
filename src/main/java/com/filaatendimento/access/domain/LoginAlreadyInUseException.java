package com.filaatendimento.access.domain;

import com.filaatendimento.common.ConflictException;

/** Throws 409 when a registration tries to use a login already taken. */
public class LoginAlreadyInUseException extends ConflictException {

    public LoginAlreadyInUseException(String login) {
        super("O login '" + login + "' já está em uso por outro usuário");
    }
}
