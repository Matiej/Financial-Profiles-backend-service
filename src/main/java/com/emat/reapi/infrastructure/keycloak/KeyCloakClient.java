package com.emat.reapi.infrastructure.keycloak;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface KeyCloakClient {

    Mono<String> createCalculatorUser(KeycloakUserRequest request);

    Flux<KeycloakUserSummary> listCalculatorUsers();

    Mono<KeycloakUserSummary> getUserById(String userId);

    Mono<KeycloakUserSummary> updateUser(String userId, KeycloakUserRequest request);

    Mono<Void> changeUserStatus(String userId, boolean isEnabled);

    Mono<Void> deleteUser(String userId);

    Mono<Void> sendUserActionsEmail(String userId, List<String> actions);
}
