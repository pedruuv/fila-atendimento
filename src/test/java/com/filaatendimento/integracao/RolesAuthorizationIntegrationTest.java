package com.filaatendimento.integracao;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Role authorization contract over real HTTP:
 * which lines answer 401 (not authenticated) and 403 (role not allowed),
 * plus ticket cancellation rules with conflict on the second cancel.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Order(2)
class RolesAuthorizationIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    private String adminToken;
    private String attendantToken;
    private String userToken;

    @BeforeAll
    void setUp() {
        adminToken = login("admin@test.fila", "SenhaAdmin@Test");
        attendantToken = login("atendente@test.fila", "SenhaAtendente@Test");

        rest.postForEntity("/api/v1/users", Map.of(
                "name", "Usuário Comun", "login", "comum@test.fila", "password", "SenhaComum@Test"), Map.class);
        userToken = login("comum@test.fila", "SenhaComum@Test");
    }

    @Test
    @DisplayName("every protected call without a token answers 401 with the standard error body")
    void protectedRouteWithoutTokenIsUnauthorized() {
        ResponseEntity<Map> response = rest.getForEntity("/api/v1/queue", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).extracting("status").isEqualTo(401);
        assertThat(response.getBody()).extracting("message").isEqualTo(
                "Acesso não autenticado. Envie um token Bearer no header Authorization.");
    }

    @Test
    @DisplayName("a tampered token is treated as unauthenticated, not as a 500")
    void tamperedTokenIsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("header.corpo-assinado-com-outra-chave");

        ResponseEntity<Map> response = rest.exchange(
                "/api/v1/queue", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a USER may issue tickets but may neither call the next one nor cancel")
    void userMayIssueButNotCallOrCancel() {
        Long ticketId = issuedByUser("Cliente do Usuário");

        assertThat(statusOf(authenticatedCall(attendantToken))).isEqualTo(HttpStatus.OK);
        ResponseEntity<Map> deniedCall = authenticatedPostWith(userToken, "/api/v1/queue/calls", null);
        assertThat(deniedCall.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> deniedCancel = authenticatedPostWith(
                userToken, "/api/v1/tickets/" + ticketId + "/cancel", null);
        assertThat(deniedCancel.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("an ATTENDANT may cancel a ticket; cancelling twice is 409 conflict")
    void attendantCancelsOnceThenConflicts() {
        Long ticketId = issuedByUser("Cliente do Atendente");

        ResponseEntity<Map> cancelled = authenticatedPostWith(
                attendantToken, "/api/v1/tickets/" + ticketId + "/cancel", null);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelled.getBody()).extracting("status").isEqualTo("CANCELLED");
        assertThat(cancelled.getBody()).extracting("cancelledAt").isNotNull();

        ResponseEntity<Map> cancelledAgain = authenticatedPostWith(
                attendantToken, "/api/v1/tickets/" + ticketId + "/cancel", null);
        assertThat(cancelledAgain.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("user listing is ADMIN only")
    void userListingIsAdminOnly() {
        assertThat(authenticatedGet(userToken, "/api/v1/users").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(authenticatedGet(attendantToken, "/api/v1/users").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<List> listing = rest.exchange("/api/v1/users", HttpMethod.GET,
                bearerRequest(adminToken, null), List.class);
        assertThat(listing.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listing.getBody()).isNotEmpty();
        assertThat(listing.getBody()).allSatisfy(user ->
                assertThat((Map<String, Object>) user).containsKeys("id", "name", "login", "role"));
    }

    @Test
    @DisplayName("registration is public but issuing still needs authentication")
    void issueWithoutTokenIs401() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/tickets", Map.of("clientName", "Sem token", "priority", false), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /* ---------- helpers ---------- */

    private String login(String login, String password) {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login", Map.of("login", login, "password", password), Map.class);
        assertThat(response.getStatusCode()).as("login helper failed").isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("token");
    }

    private Long issuedByUser(String clientName) {
        ResponseEntity<Map> response = authenticatedPostWith(
                userToken, "/api/v1/tickets", Map.of("clientName", clientName, "priority", false));
        assertThat(response.getStatusCode()).as("issue helper failed").isEqualTo(HttpStatus.CREATED);
        return ((Number) response.getBody().get("id")).longValue();
    }

    /** Calls the next ticket and returns the raw status (200 when someone was waiting). */
    private HttpStatusCode statusOf(ResponseEntity<Map> response) {
        return response.getStatusCode();
    }

    private ResponseEntity<Map> authenticatedCall(String token) {
        return rest.exchange("/api/v1/queue/calls", HttpMethod.POST,
                bearerRequest(token, null), Map.class);
    }

    private ResponseEntity<Map> authenticatedPostWith(String token, String url, Object body) {
        return rest.exchange(url, HttpMethod.POST, bearerRequest(token, body), Map.class);
    }

    private ResponseEntity<Map> authenticatedGet(String token, String url) {
        return rest.exchange(url, HttpMethod.GET, bearerRequest(token, null), Map.class);
    }

    private HttpEntity<Object> bearerRequest(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }
}
