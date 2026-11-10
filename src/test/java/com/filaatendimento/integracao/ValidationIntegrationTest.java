package com.filaatendimento.integracao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Input validation contract: invalid payloads answer 400 with per-field
 * details in the standard error shape, never a 500.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Order(3)
class ValidationIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("registering with a short password answers 400 listing the field problem")
    void shortPasswordIsRejectedWithFieldDetails() {
        ResponseEntity<Map> response = rest.postForEntity("/api/v1/users", Map.of(
                "name", "Cliente Curto", "login", "curto@test.fila", "password", "curta"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).extracting("status").isEqualTo(400);
        Map<String, Object> errors = (Map<String, Object>) response.getBody().get("errors");
        assertThat(errors).containsKey("password");
    }

    @Test
    @DisplayName("emitting a ticket without the priority field is 400, never an internal error")
    void issuingTicketWithoutPriorityIsBadRequest() {
        // First, login as an attendant to have a valid token.
        ResponseEntity<Map> login = rest.postForEntity("/api/v1/auth/login", Map.of(
                "login", "atendente@test.fila", "password", "SenhaAtendente@Test"), Map.class);
        String token = (String) login.getBody().get("token");
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<Map> response = rest.exchange("/api/v1/tickets",
                org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(Map.of("clientName", "Sem prioridade"), headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> errors = (Map<String, Object>) response.getBody().get("errors");
        assertThat(errors).containsKey("priority");
    }

    @Test
    @DisplayName("login with blank credentials answers 400 with field details")
    void loginWithBlankFieldsIsBadRequest() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/v1/auth/login", Map.of("login", "", "password", ""), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, Object> errors = (Map<String, Object>) response.getBody().get("errors");
        assertThat(errors).containsKeys("login", "password");
    }
}
