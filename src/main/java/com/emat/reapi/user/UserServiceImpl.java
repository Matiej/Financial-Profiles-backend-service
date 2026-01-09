package com.emat.reapi.user;

import com.emat.reapi.infrastructure.keycloak.KeyCloakClient;
import com.emat.reapi.infrastructure.keycloak.KeycloakUserSummary;
import com.emat.reapi.user.domain.CreateUserRequest;
import com.emat.reapi.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final KeyCloakClient keyCloakClient;

    @Override
    public Flux<User> getAllCalculatorUsers() {
        return keyCloakClient.listCalculatorUsers()
                .map(KeycloakUserSummary::toDomain)
                .map(User::fromKeycloak)
                .doOnError(err -> log.warn("Error fetched calculator users"));
    }

    @Override
    public Mono<String> createCalculatorUser(CreateUserRequest request) {
        return keyCloakClient.createCalculatorUser(request.toKeycloakRequest())
                .doOnSuccess(suc -> log.info("User {}, created successfully", request.username()));
    }

    @Override
    public Mono<User> updateUser(String userId, CreateUserRequest request) {
        return keyCloakClient.updateUser(userId, request.toKeycloakRequest())
                .map(KeycloakUserSummary::toDomain)
                .map(User::fromKeycloak)
                .doOnSuccess(suc -> log.info("User id: '{}' has been updated successfully", userId));
    }

    @Override
    public Mono<Void> deleteUser(String userId) {
        return keyCloakClient.deleteUser(userId)
                .doOnSuccess(suc -> log.info("User id: '{}' successfully deleted", userId));
    }

    @Override
    public Mono<Void> changeUserStatus(String userId, boolean isEnabled) {
        return keyCloakClient.changeUserStatus(userId, isEnabled)
                .doOnSuccess(suc -> log.info("User id:'{}' status successfully changed to: {}", userId, isEnabled));
    }
}
