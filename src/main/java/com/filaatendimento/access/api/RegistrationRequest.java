package com.filaatendimento.access.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload required to register a new self-service user (role USER). */
@Schema(description = "Dados necessários para cadastrar um novo usuário")
public record RegistrationRequest(
        @Schema(description = "Nome completo do usuário", example = "Maria Silva", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @Schema(description = "Login único para autenticação", example = "maria.silva", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "O login é obrigatório")
        @Size(max = 120, message = "O login deve ter no máximo 120 caracteres")
        String login,

        @Schema(description = "Senha de acesso (mínimo de 8 caracteres)", example = "senhaSegura123", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8)
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres")
        String password
) {
}
