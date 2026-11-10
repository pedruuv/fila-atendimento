package com.filaatendimento.access.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Credentials required to authenticate and receive a Bearer token. */
@Schema(description = "Credenciais necessárias para autenticação")
public record LoginRequest(
        @Schema(description = "Login cadastrado do usuário", example = "admin@fila.dev", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O login é obrigatório") String login,

        @Schema(description = "Senha de acesso do usuário", example = "minhaSenhaSegura123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "A senha é obrigatória") String password
) {
}
