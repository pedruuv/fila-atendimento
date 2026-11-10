package com.filaatendimento.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filaatendimento.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes JSON error responses when Spring Security rejects a request:
 * 401 when the caller is not authenticated (or the token is invalid),
 * 403 when the caller is authenticated but lacks the required role.
 */
@Component
public class SecurityRejectionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityRejectionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Called when an unauthenticated caller reaches a protected route. */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        writeRejection(response, HttpStatus.UNAUTHORIZED,
                "Acesso não autenticado. Envie um token Bearer no header Authorization.", request.getRequestURI());
    }

    /** Called when an authenticated caller lacks the required role for the route. */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        writeRejection(response, HttpStatus.FORBIDDEN,
                "Acesso negado: o papel do usuário não permite esta operação.", request.getRequestURI());
    }

    private void writeRejection(HttpServletResponse response, HttpStatus status, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.withoutFieldDetails(status, message, path);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
