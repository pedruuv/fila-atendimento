package com.filaatendimento.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates business exceptions into the API error responses,
 * guaranteeing that every client receives the same {@link ErrorResponse} shape, in JSON.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingResource(
            NotFoundException exception, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictingState(
            ConflictException exception, HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidAccessTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAccessToken(
            InvalidAccessTokenException exception, HttpServletRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInputValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errorsByField = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errorsByField.put(error.getField(), error.getDefaultMessage()));
        ErrorResponse response = new ErrorResponse(
                java.time.Instant.now(), HttpStatus.BAD_REQUEST.value(),
                "Requisição contém dados inválidos", request.getRequestURI(), errorsByField);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(
            Exception exception, HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST,
                "Corpo ou parâmetros da requisição estão malformados", request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnknownRoute(
            NoResourceFoundException exception, HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "Rota não encontrada", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(
            Exception exception, HttpServletRequest request) {
        logger.error("Erro inesperado ao processar {}", request.getRequestURI(), exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor. Tente novamente mais tarde.", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.withoutFieldDetails(status, message, path));
    }
}
