package com.filaatendimento.attendance.api;

import com.filaatendimento.attendance.application.QueueService;
import com.filaatendimento.attendance.domain.Ticket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST interface for issuing, consulting and managing single tickets. */
@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Senhas", description = "Operações sobre senhas individuais da fila de atendimento")
public class TicketController {

    private final QueueService queueService;

    public TicketController(QueueService queueService) {
        this.queueService = queueService;
    }

    @Operation(summary = "Emitir nova senha",
            description = "Cria uma nova senha para o cliente informado, na fila comum ou prioritária.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Senha emitida com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token Bearer ausente ou inválido", content = @Content)
    })
    @PostMapping
    public ResponseEntity<TicketResponse> issueTicket(@Valid @RequestBody TicketIssueRequest ticketIssueRequest) {
        Ticket issuedTicket = queueService.issueTicket(
                ticketIssueRequest.clientName(), ticketIssueRequest.priority());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TicketResponse.fromDomain(issuedTicket, queueService.queuePositionOf(issuedTicket)));
    }

    @Operation(summary = "Consultar senha por id",
            description = "Retorna os dados da senha, incluindo a posição na fila quando ainda está aguardando.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha encontrada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "404", description = "Senha não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token Bearer ausente ou inválido", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> findTicketById(@PathVariable Long id) {
        Ticket ticket = queueService.findTicketById(id);
        return ResponseEntity.ok(TicketResponse.fromDomain(ticket, queueService.queuePositionOf(ticket)));
    }

    @Operation(summary = "Consultar senha por posição na fila",
            description = "Retorna a senha que ocupa a posição informada (1-based) na fila comum ou prioritária.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha encontrada na posição informada",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "404", description = "Não existe senha aguardando nessa posição",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token Bearer ausente ou inválido", content = @Content)
    })
    @GetMapping("/position/{position}")
    public ResponseEntity<TicketResponse> findTicketInQueuePosition(
            @Parameter(description = "Posição na fila, começando em 1", example = "1")
            @PathVariable int position,
            @Parameter(description = "true consulta a fila prioritária; false a comum", example = "false")
            @RequestParam(defaultValue = "false") boolean priority) {

        Ticket ticket = queueService.findTicketInQueuePosition(position, priority);
        return ResponseEntity.ok(TicketResponse.fromDomain(ticket, queueService.queuePositionOf(ticket)));
    }

    @Operation(summary = "Cancelar senha",
            description = "Cancela a senha mantendo o registro no banco. Só é permitido enquanto a senha ainda "
                    + "não foi cancelada (AGUARDANDO ou CHAMADA). Requer papel ATENDENTE ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha cancelada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "404", description = "Senha não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Senha já estava cancelada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token Bearer ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Papel sem permissão para cancelar", content = @Content)
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(@PathVariable Long id) {
        Ticket cancelledTicket = queueService.cancelTicket(id);
        return ResponseEntity.ok(TicketResponse.fromDomain(cancelledTicket, queueService.queuePositionOf(cancelledTicket)));
    }

    @Operation(summary = "Excluir senha permanentemente",
            description = "Remove o registro do banco de dados, sem histórico. Requer papel ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha excluída com sucesso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Senha não encontrada", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token Bearer ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Papel sem permissão para excluir", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        queueService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
