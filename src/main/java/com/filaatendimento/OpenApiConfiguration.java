package com.filaatendimento;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata, documenting the Bearer token scheme for the UI. */
@Configuration
public class OpenApiConfiguration {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI queueAttendanceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fila de Atendimento API")
                        .description("""
                                API para gerenciar uma fila de espera com senhasnormais e prioritárias.

                                Autenticação: a maioria das rotas exige um JWT no header \\
                                `Authorization: Bearer <token>`. Obtenha o token via \\
                                `POST /api/v1/auth/login` ou cadastre-se em `POST /api/v1/users`.
                                """)
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME, new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Informe o token Bearer retornado no login.")));
    }
}
