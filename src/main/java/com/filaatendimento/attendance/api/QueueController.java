package com.filaatendimento.attendance.api;

import com.filaatendimento.attendance.application.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST interface for the attendance queue itself: the live state of the
 * lines, the next call made by an attendant and the recent call history.
 */
@RestController
@RequestMapping("/api/v1/queue")
@Tag(name = "Fila", description = "Consulta do estado da fila e chamada de senhas")
public class QueueController {

    private static final int MAXIMUM_RECENT_CALLS_LIMIT = 100;

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    @Operation(summary = "Estado atual da fila",
            description = "Lista as senhas aguardando, primeiro as prioritárias e depois as comuns, "
                    + "e os totais de cada fila.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fila consultada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = QueueStateResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token Bearer ausente ou inválido", content = @Content)
    })
    @GetMapping
    public ResponseEntity<QueueStateResponse> queueState() {
        List<TicketResponse> waitingPriority = queueService.listWaitingTickets(true).stream()
                .map(TicketResponse::withoutPosition)
                .toList();
        List<TicketResponse> waitingRegular = queueService.listWaitingTickets(false).stream()
                .map(TicketResponse::withoutPosition)
                .toList();

        List<TicketResponse> waitingTickets =
                java.util.stream.Stream.concat(waitingPriority.stream(), waitingRegular.stream()).toList();
        QueueStateResponse response = QueueStateResponse.of(
                waitingTickets, waitingPriority.size(), waitingRegular.size());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Chamar a próxima senha",
            description = "Chama a próxima senha aplicando a política de justiça prioritária (as prioritárias "
                    + "entram em sequência até o teto configurado de chamadas consecutivas). Sem corpo quando "
                    + "não há ninguém aguardando. Requer papel ATENDENTE ou ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha chamada com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TicketResponse.class))),
            @ApiResponse(responseCode = "204", description = "Ninguém aguardando em nenhuma fila",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token Bearer ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Papel sem permissão para chamar", content = @Content)
    })
    @PostMapping("/calls")
    public ResponseEntity<TicketResponse> callNextTicket() {
        return queueService.callNextTicket()
                .map(ticket -> ResponseEntity.ok(TicketResponse.withoutPosition(ticket)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Histórico das últimas chamadas",
            description = "Lista as senhas chamadas mais recentemente, da mais nova para a mais antiga.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chamadas listadas com sucesso",
                    content = @Content(mediaType = "application/json", array =
                            @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = TicketResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Token Bearer ausente ou inválido", content = @Content)
    })
    @GetMapping("/calls")
    public ResponseEntity<List<TicketResponse>> recentCalls(
            @Parameter(description = "Quantidade máxima de chamadas (1 a 100)", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        int boundedLimit = Math.clamp(limit, 1, MAXIMUM_RECENT_CALLS_LIMIT);
        List<TicketResponse> calls = queueService.listRecentCalls(boundedLimit).stream()
                .map(TicketResponse::withoutPosition)
                .toList();
        return ResponseEntity.ok(calls);
    }

    @Operation(summary = "Limpar a fila",
            description = "Cancela todas as senhas aguardando nas duas filas, mantendo os registros no banco. "
                    + "Requer papel ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fila limpa com sucesso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = QueueClearingResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token Bearer ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Papel sem permissão para limpar a fila",
                    content = @Content)
    })
    @PostMapping("/clear")
    public ResponseEntity<QueueClearingResponse> clearQueue() {
        long cancelledAmount = queueService.clearQueue().size();
        return ResponseEntity.ok(new QueueClearingResponse(cancelledAmount));
    }
}
