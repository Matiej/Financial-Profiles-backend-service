package com.emat.reapi.user.infra;

import com.emat.reapi.user.domain.KeycloakUserRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KeyCloakClient {

    Mono<String> createCalculatorUser(KeycloakUserRequest request);

    Flux<KeycloakUserSummary> listCalculatorUsers();

    Mono<KeycloakUserSummary> updateCalculatorUser(String userId, KeycloakUserRequest request);

    Mono<Void> disableCalculatorUser(String userId);

    Mono<Void> deleteCalculatorUser(String userId);
}
