package com.filaatendimento.access.api;

import com.filaatendimento.access.application.UserRegistrationService;
import com.filaatendimento.access.domain.User;
import com.filaatendimento.access.persistence.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public registration of new users plus the ADMIN-only user listing.
 * Registration always creates USER role accounts; privileged roles
 * (ATTENDANT, ADMIN) exist only through the initial bootstrap configuration.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Cadastro e consulta de usuários da API")
public class UserController {

    private final UserRegistrationService registrationService;
    private final UserRepository userRepository;

    public UserController(UserRegistrationService registrationService, UserRepository userRepository) {
        this.registrationService = registrationService;
        this.userRepository = userRepository;
    }

    @Operation(summary = "Cadastrar usuário", description = "Cria um novo usuário público com papel USER.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (campos ausentes ou senha fraca)",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Login já utilizado por outro usuário",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest registrationRequest) {
        User registeredUser = registrationService.register(
                registrationRequest.name(), registrationRequest.login(), registrationRequest.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.fromDomain(registeredUser));
    }

    @Operation(summary = "Listar usuários", description = "Disponível apenas para administradores.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuários listados com sucesso",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(
                            schema = @Schema(implementation = UserResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão de administrador", content = @Content)
    })
    @GetMapping
    public List<UserResponse> list() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromDomain)
                .toList();
    }
}
