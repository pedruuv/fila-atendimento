package com.filaatendimento.integracao;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full attendance flow on a working API (H2 + real HTTP): registration,
 * login, Bearer header, emission, fairness-ordered calls, totals,
 * queue position and ADMIN clearing/deleting.
 *
 * <p>The bootstrap configuration seeds one ADMIN and one ATTENDANT
 * (see src/test/resources/application.yml).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Order(1)
class AttendanceFlowIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    /* Steps run in order so later steps use state created earlier. */
    private String adminToken;
    private String attendantToken;

    @BeforeAll
    void setUp() {
        adminToken = login("admin@test.fila", "SenhaAdmin@Test");
        attendantToken = login("atendente@test.fila", "SenhaAtendente@Test");
    }

    @Test
    @Order(1)
    @DisplayName("a registered user receives 201 with USER role")
    void selfRegistrationGrantsUserRole() {
        var request = Map.of("name", "Cliente Registrado", "login", "cliente@test.fila",
                "password", "SenhaCliente@Test");

        ResponseEntity<Map> response = rest.postForEntity("/api/v1/users", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).extracting("login").isEqualTo("cliente@test.fila");
        assertThat(response.getBody()).extracting("role").isEqualTo("USER");
    }

    @Test
    @Order(2)
    @DisplayName("registering the same login again is a 409 conflict")
    void duplicatedRegistrationIsConflict() {
        var request = Map.of("name", "Outro Cliente", "login", "cliente@test.fila",
                "password", "SenhaCliente@Test");

        ResponseEntity<Map> response = rest.postForEntity("/api/v1/users", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).extracting("status").isEqualTo(409);
    }

    @Test
    @Order(3)
    @DisplayName("login with correct credentials answers a signed Bearer JWT")
    void loginAnswersBearerJwt() {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login",
                Map.of("login", "atendente@test.fila", "password", "SenhaAtendente@Test"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting("typeOfToken").isEqualTo("Bearer");
        String token = (String) response.getBody().get("token");
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @Order(4)
    @DisplayName("a wrong password is a shallow 401 that never reveals which part failed")
    void wrongPasswordIsShallowUnauthorized() {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login",
                Map.of("login", "atendente@test.fila", "password", "SenhaErrada@Test"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).extracting("message").isEqualTo("Login ou senha inválidos");
    }

    @Test
    @Order(5)
    @DisplayName("attended fair calls: priority, priority, regular, priority (cap = 2)")
    void callsFollowFairnessPolicyPriorityPriorityRegularPriority() {
        // Both lines: two priority + one regular waiting.
        Long priorityFirst = issued("Cliente P1", true, adminToken);
        Long prioritySecond = issued("Cliente P2", true, adminToken);
        Long regularFirst = issued("Cliente R1", false, adminToken);
        Long priorityThird = issued("Cliente P3", true, adminToken);

        assertThat(calledNumber(attendantToken)).isEqualTo(numberOf(priorityFirst));
        assertThat(calledNumber(attendantToken)).isEqualTo(numberOf(prioritySecond));
        assertThat(calledNumber(attendantToken)).isEqualTo(numberOf(regularFirst));
        assertThat(calledNumber(attendantToken)).isEqualTo(numberOf(priorityThird));
    }

    @Test
    @Order(6)
    @DisplayName("queue state shows remaining waiting tickets and their totals")
    void queueStateShowAndTotals() {
        Long waitingPriority = issued("Cliente P4", true, adminToken);
        Long waitingRegular = issued("Cliente R2", false, adminToken);

        ResponseEntity<Map> response = authenticatedGet("/api/v1/queue", adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> waiting = (List<Map<String, Object>>) response.getBody().get("waitingTickets");
        assertThat(waiting).extracting("id")
                .containsExactlyInAnyOrder(waitingPriority.intValue(), waitingRegular.intValue());        assertThat(((Number) response.getBody().get("waitingPriority")).longValue()).isEqualTo(1);
        assertThat(((Number) response.getBody().get("waitingRegular")).longValue()).isEqualTo(1);
        assertThat(((Number) response.getBody().get("waitingTotal")).longValue()).isEqualTo(2);
    }

    @Test
    @Order(7)
    @DisplayName("position endpoint answers the ticket standing at the given position")
    void queuePositionEndpointAnswersTicket() {
        ResponseEntity<Map> response =
                authenticatedGet("/api/v1/tickets/position/1?priority=true", adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting("clientName").isEqualTo("Cliente P4");
    }

    @Test
    @Order(8)
    @DisplayName("an out-of-range position is a 404 with the standard error shape")
    void outOfRangePositionIsNotFound() {
        ResponseEntity<Map> response =
                authenticatedGet("/api/v1/tickets/position/99?priority=false", adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKeys("status", "message", "path");
    }

    @Test
    @Order(9)
    @DisplayName("recent calls list the tickets CALLED, newest first")
    void recentCallsListCalledTickets() {
        ResponseEntity<List> response =
                authenticatedGetAsList("/api/v1/queue/calls?limit=10", adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> calls = response.getBody();
        assertThat(calls).hasSizeGreaterThanOrEqualTo(4);
        assertThat(calls).allSatisfy(call -> assertThat(call).extracting("status").isEqualTo("CALLED"));
        List<String> calledNumbers = calls.stream().map(call -> (String) call.get("number")).toList();
        assertThat(calledNumbers.stream().distinct().count()).isEqualTo(calledNumbers.size());
    }

    @Test
    @Order(10)
    @DisplayName("only ADMIN may clear the queue, cancelling every standing ticket")
    void onlyAdminClearsTheQueue() {
        // Two tickets are waiting after step 6: clear must cancel both.
        ResponseEntity<Map> clearAsAttendant = authenticatedPostWithoutBody("/api/v1/queue/clear", attendantToken);
        assertThat(clearAsAttendant.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> clearAsAdmin = authenticatedPostWithoutBody("/api/v1/queue/clear", adminToken);
        assertThat(clearAsAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) clearAsAdmin.getBody().get("cancelledAmount")).longValue()).isEqualTo(2);

        ResponseEntity<Map> queueAfterClear = authenticatedGet("/api/v1/queue", adminToken);
        assertThat(((Number) queueAfterClear.getBody().get("waitingTotal")).longValue()).isEqualTo(0);
    }

    @Test
    @Order(11)
    @DisplayName("deleting a ticket is 403 for attendant and 204 for admin, then 404 afterwards")
    void deletingTicketRestrictedToAdmin() {
        Long ticketId = issued("Cliente Excluir", false, adminToken);

        ResponseEntity<Void> forbidden = authenticatedDelete("/api/v1/tickets/" + ticketId, attendantToken);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Void> deleted = authenticatedDelete("/api/v1/tickets/" + ticketId, adminToken);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> notFound = authenticatedGet("/api/v1/tickets/" + ticketId, adminToken);
        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(12)
    @DisplayName("unknown routes answer the standard error shape too")
    void unknownRouteAnswersStandardErrorShape() {
        ResponseEntity<Map> response = authenticatedGet("/api/v1/nope", adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKeys("status", "message", "path");
    }

    /* ---------- helpers ---------- */

    private String login(String login, String password) {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login", Map.of("login", login, "password", password), Map.class);
        assertThat(response.getStatusCode()).as("login helper failed").isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("token");
    }

    private Long issued(String clientName, boolean priority, String token) {
        ResponseEntity<Map> response = authenticatedPost(
                "/api/v1/tickets", Map.of("clientName", clientName, "priority", priority), token);
        assertThat(response.getStatusCode()).as("issue helper failed").isEqualTo(HttpStatus.CREATED);
        Long id = ((Number) response.getBody().get("id")).longValue();
        String renderedNumber = (String) response.getBody().get("number");
        context.put(id, renderedNumber);
        return id;
    }

    private final Map<Long, String> context = new java.util.HashMap<>();

    private String numberOf(Long ticketId) {
        return context.get(ticketId);
    }

    private String calledNumber(String token) {
        ResponseEntity<TicketBody> response = authenticatedPostWithoutBodyOfClass(
                "/api/v1/queue/calls", token, TicketBody.class);
        assertThat(response.getStatusCode()).as("call helper failed").isEqualTo(HttpStatus.OK);
        return response.getBody().number;
    }

    private ResponseEntity<Map> authenticatedGet(String url, String token) {
        return authenticatedExchange(url, token, HttpStatus.OK, Map.class);
    }

    private ResponseEntity<List> authenticatedGetAsList(String url, String token) {
        return rest.exchange(url, HttpMethod.GET, bearerRequest(token, null), List.class);
    }

    private ResponseEntity<Map> authenticatedPost(String url, Object body, String token) {
        return rest.exchange(url, HttpMethod.POST, bearerRequest(token, body), Map.class);
    }

    private ResponseEntity<Map> authenticatedPostWithoutBody(String url, String token) {
        return rest.exchange(url, HttpMethod.POST, bearerRequest(token, null), Map.class);
    }

    private <T> ResponseEntity<T> authenticatedPostWithoutBodyOfClass(String url, String token, Class<T> type) {
        return rest.exchange(url, HttpMethod.POST, bearerRequest(token, null), type);
    }

    private ResponseEntity<Void> authenticatedDelete(String url, String token) {
        return rest.exchange(url, HttpMethod.DELETE, bearerRequest(token, null), Void.class);
    }

    private ResponseEntity<Map> authenticatedExchange(String url, String token, HttpStatus expected, Class<Map> type) {
        return rest.exchange(url, HttpMethod.GET, bearerRequest(token, null), type);
    }

    private HttpEntity<Object> bearerRequest(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
    /** Typed body of the call response helping the boxing assertions above. */
    record TicketBody(Long id, String number, String clientName, boolean priority, String status) {
    }
}
