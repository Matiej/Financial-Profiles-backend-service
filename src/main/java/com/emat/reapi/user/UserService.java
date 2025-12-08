package com.emat.reapi.user;

import com.emat.reapi.user.domain.KeycloakUser;
import com.emat.reapi.user.domain.KeycloakUserRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Flux<KeycloakUser> getAllCalculatorUsers();
    Mono<String> createCalculatorUser(KeycloakUserRequest request);
    Mono<KeycloakUser> updateUser(String userId, KeycloakUserRequest request);
    Mono<Void> deleteUser(String userId);
    Mono<Void> changeUserStatus(String userId, boolean isEnabled);
}
