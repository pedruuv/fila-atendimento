package com.filaatendimento.access.api;

import com.filaatendimento.access.application.AuthenticationService;
import com.filaatendimento.access.application.AuthenticationToken;
import com.filaatendimento.security.JwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Authentication entry point: validates credentials and issues the Bearer token. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Login e emissão de token JWT Bearer")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtProperties jwtProperties;

    public AuthenticationController(AuthenticationService authenticationService, JwtProperties jwtProperties) {
        this.authenticationService = authenticationService;
        this.jwtProperties = jwtProperties;
    }

    @Operation(
            summary = "Autenticar usuário",
            description = "Valida login e senha e retorna um token Bearer para acessar as rotas protegidas.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Login ou senha inválidos",
                    content = @Content)
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthenticationToken token = authenticationService.login(loginRequest.login(), loginRequest.password());
        return new LoginResponse(token.value(), token.typeOfToken(), jwtProperties.validitySeconds());
    }
}
