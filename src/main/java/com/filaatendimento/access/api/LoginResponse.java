package com.filaatendimento.access.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** Data returned after a successful authentication. */
@Schema(description = "Dados retornados após autenticação bem-sucedida")
public record LoginResponse(
        @Schema(description = "Token JWT gerado para o usuário autenticado", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,

        @Schema(description = "Tipo do token de autenticação", example = "Bearer")
        String typeOfToken,

        @Schema(description = "Quantidade de segundos de validade do token", example = "3600")
        long validityInSeconds
) {
}
