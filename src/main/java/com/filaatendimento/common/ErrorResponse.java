package com.filaatendimento.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Standard shape of every API error response, produced by
 * {@link GlobalExceptionHandler}.
 *
 * @param occurredAt instant when the error was detected
 * @param status     HTTP status code of the response
 * @param message    human-readable description of the problem
 * @param path       URI of the request that caused the error
 * @param errors     per-field validation details, when the cause is input validation
 */
@Schema(description = "Resposta padrão de erro da API")
public record ErrorResponse(
        @Schema(description = "Momento em que o erro ocorreu", example = "2026-11-10T13:22:31Z") Instant occurredAt,
        @Schema(description = "Código HTTP do erro", example = "404") int status,
        @Schema(description = "Descrição do problema", example = "Senha não encontrada") String message,
        @Schema(description = "Caminho da requisição que gerou o erro", example = "/api/v1/tickets/42") String path,
        @Schema(description = "Erros de validação por campo, quando houver") Object errors
) {

    public static ErrorResponse withoutFieldDetails(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now(), status.value(), message, path, null);
    }
}
