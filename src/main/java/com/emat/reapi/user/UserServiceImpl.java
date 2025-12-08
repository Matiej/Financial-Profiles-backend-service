package com.emat.reapi.user;

import com.emat.reapi.user.domain.KeycloakUser;
import com.emat.reapi.user.domain.KeycloakUserRequest;
import com.emat.reapi.user.infra.KeyCloakClient;
import com.emat.reapi.user.infra.KeycloakUserSummary;
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
    public Flux<KeycloakUser> getAllCalculatorUsers() {
        return keyCloakClient.listCalculatorUsers()
                .map(KeycloakUserSummary::toDomain)
                .doOnError(err -> log.warn("Error fetched calculator users"));
    }

    @Override
    public Mono<String> createCalculatorUser(KeycloakUserRequest request) {
        return keyCloakClient.createCalculatorUser(request)
                .doOnSuccess(suc -> log.info("User {}, created successfully", request.username()));
    }

    @Override
    public Mono<KeycloakUser> updateUser(String userId, KeycloakUserRequest request) {
        return keyCloakClient.updateUser(userId, request)
                .map(KeycloakUserSummary::toDomain)
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
