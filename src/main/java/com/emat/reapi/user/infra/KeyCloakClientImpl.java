package com.emat.reapi.user.infra;

import com.emat.reapi.user.KeycloakException;
import com.emat.reapi.user.domain.KeycloakUserRequest;
import com.emat.reapi.user.keycloakconfing.KeycloakAdminProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
class KeyCloakClientImpl implements KeyCloakClient {

    private final WebClient keycloakAdminWebClient;
    private final KeycloakAdminTokenProvider tokenProvider;
    private final KeycloakAdminProperties properties;

    @Override
    public Mono<String> createCalculatorUser(KeycloakUserRequest request) {
        return tokenProvider.getToken()
                .flatMap(token ->
                        keycloakAdminWebClient.post()
                                .uri("/users")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of(
                                        "username", request.username(),
                                        "email", request.email(),
                                        "firstName", request.firstName(),
                                        "lastName", request.lastName(),
                                        "enabled", true,
                                        "emailVerified", false
                                ))
                                .exchangeToMono(response -> {
                                    if (response.statusCode().is2xxSuccessful()) {
                                        URI location = response.headers().asHttpHeaders().getLocation();
                                        String id = location != null
                                                ? location.getPath().substring(location.getPath().lastIndexOf('/') + 1)
                                                : null;
                                        return Mono.just(id);
                                    }
                                    return response.bodyToMono(String.class)
                                            .defaultIfEmpty("")
                                            .flatMap(body -> {
                                                HttpStatus status = (HttpStatus) response.statusCode();

                                                if (status == HttpStatus.CONFLICT) {
                                                    return Mono.error(new KeycloakException(
                                                            "User creation failed. User with this email or username already exists.",
                                                            KeycloakException.KeyCloakExceptionErrorType.USER_ALREADY_EXISTS,
                                                            status
                                                    ));
                                                }

                                                return Mono.error(new KeycloakException(
                                                        "Keycloak error " + status.value() + ": " + body,
                                                        KeycloakException.KeyCloakExceptionErrorType.GENERIC_ERROR,
                                                        status
                                                ));
                                            });
                                })
                )
                .flatMap(userId ->
                        assignCalculatorRole(userId)
                                .then(sendVerifyEmail(userId))
                                .thenReturn(userId)
                );
    }

    private Mono<Void> assignCalculatorRole(String userId) {
        var roles = Map.of(
                "id", properties.calculatorRoleId(),
                "name", properties.calculatorRoleName()
        );
        return tokenProvider.getToken()
                .flatMap(token ->
                        keycloakAdminWebClient.post()
                                .uri("/users/{id}/role-mappings/realm", userId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(List.of(roles))
                                .retrieve()
                                .bodyToMono(Void.class)
                );
    }

    private Mono<Void> sendVerifyEmail(String userId) {
        return tokenProvider.getToken()
                .flatMap(token ->
                        keycloakAdminWebClient.put()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/users/{id}/execute-actions-email")
                                        .queryParam("client_id", properties.frontendClientId())
                                        .build(userId)
                                )
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(List.of("VERIFY_EMAIL"))
                                .retrieve()
                                .bodyToMono(Void.class)
                );
    }

    @Override
    public Flux<KeycloakUserSummary> listCalculatorUsers() {
        return tokenProvider.getToken()
                .flatMapMany(token ->
                        keycloakAdminWebClient.get()
                                .uri("/roles/{roleName}/users", properties.calculatorRoleName())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .onStatus(
                                        HttpStatusCode::isError,
                                        response -> response.bodyToMono(String.class)
                                                .defaultIfEmpty("")
                                                .flatMap(body -> {
                                                    HttpStatus status = (HttpStatus) response.statusCode();

                                                    log.error(
                                                            "Keycloak listCalculatorUsers error. Status: {}, body: {}",
                                                            status.value(),
                                                            body
                                                    );

                                                    return Mono.error(
                                                            new KeycloakException(
                                                                    "Error fetching calculator users from Keycloak. " +
                                                                            "HTTP status: " + status.value(),
                                                                    KeycloakException.KeyCloakExceptionErrorType.LIST_CALCULATOR_USER_ERROR,
                                                                    status
                                                            )
                                                    );
                                                })
                                )
                                .bodyToFlux(JsonNode.class)
                                .map(node -> new KeycloakUserSummary(
                                        node.get("id").asText(),
                                        node.get("username").asText(),
                                        node.path("firstName").asText(null),
                                        node.path("lastName").asText(null),
                                        node.hasNonNull("email") ? node.get("email").asText() : null,
                                        node.get("enabled").asBoolean(),
                                        node.get("emailVerified").asBoolean(),
                                        Instant.ofEpochMilli(node.get("createdTimestamp").asLong())
                                ))
                );
    }

    @Override
    public Mono<KeycloakUserSummary> updateCalculatorUser(String userId, KeycloakUserRequest request) {
        return tokenProvider.getToken()
                .flatMap(token ->
                        keycloakAdminWebClient.get()
                                .uri("/users/{id}", userId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(JsonNode.class)
                                .flatMap(existing -> {
                                    ((ObjectNode) existing).put("username", request.username());
                                    ((ObjectNode) existing).put("firstName", request.firstName());
                                    ((ObjectNode) existing).put("lastName", request.lastName());
                                    ((ObjectNode) existing).put("email", request.email());
                                    ((ObjectNode) existing).put("enabled", request.enabled());
                                    return keycloakAdminWebClient.put()
                                            .uri("/users/{id}", userId)
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .bodyValue(existing)
                                            .retrieve()
                                            .bodyToMono(Void.class)
                                            .thenReturn(existing);
                                })
                ).map(node -> new KeycloakUserSummary
                        (
                                node.get("id").asText(),
                                node.get("username").asText(),
                                node.get("firstName").asText(),
                                node.get("lastName").asText(),
                                node.hasNonNull("email") ? node.get("email").asText() : null,
                                node.get("enabled").asBoolean(),
                                node.get("emailVerified").asBoolean(),
                                Instant.ofEpochMilli(node.get("createdTimestamp").asLong())
                        )
                );
    }

    @Override
    public Mono<Void> disableCalculatorUser(String userId) {
        return tokenProvider.getToken()
                .flatMap(token ->
                        keycloakAdminWebClient.get()
                                .uri("/users/{id}", userId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(JsonNode.class)
                                .flatMap(existing -> {
                                    ((ObjectNode) existing).put("enabled", false);
                                    return keycloakAdminWebClient.put()
                                            .uri("/users/{id}", userId)
                                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .bodyValue(existing)
                                            .retrieve()
                                            .bodyToMono(Void.class);
                                })
                );
    }

    @Override
    public Mono<Void> deleteCalculatorUser(String userId) {
        return tokenProvider.getToken()
                .flatMap(token ->
                        keycloakAdminWebClient.delete()
                                .uri("/users/{id}", userId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(Void.class)
                );
    }
}
