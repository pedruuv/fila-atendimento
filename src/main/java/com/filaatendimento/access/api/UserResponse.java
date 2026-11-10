package com.filaatendimento.access.api;

import com.filaatendimento.access.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Public representation of a user, never exposing password data. */
@Schema(description = "Dados do usuário cadastrado")
public record UserResponse(
        @Schema(description = "Identificador do usuário", example = "1")
        Long id,

        @Schema(description = "Nome do usuário", example = "Maria Silva")
        String name,

        @Schema(description = "Login do usuário", example = "maria.silva")
        String login,

        @Schema(description = "Papel do usuário na API", example = "USER")
        String role
) {

    public static UserResponse fromDomain(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getLogin(), user.getRole().name());
    }
}
