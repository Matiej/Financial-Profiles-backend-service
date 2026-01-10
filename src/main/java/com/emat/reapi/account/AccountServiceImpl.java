package com.emat.reapi.account;

import com.emat.reapi.account.domain.Account;
import com.emat.reapi.account.domain.UpdateProfileCommand;
import com.emat.reapi.infrastructure.keycloak.KeyCloakClient;
import com.emat.reapi.infrastructure.keycloak.KeycloakUserSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final KeyCloakClient keyCloakClient;

    @Override
    public Mono<Account> getAccount(String userId) {
        return keyCloakClient.getUserById(userId)
                .map(KeycloakUserSummary::toDomain)
                .map(Account::fromKeycloak)
                .doOnSuccess(account -> log.info("Account retrieved for userId: {}", userId))
                .doOnError(err -> log.warn("Error retrieving account for userId: {}", userId));
    }

    @Override
    public Mono<Account> updateProfile(String userId, UpdateProfileCommand command) {
        return keyCloakClient.getUserById(userId)
                .flatMap(existing -> {
                    var request = command.toKeycloakRequest(
                            existing.username(),
                            existing.enabled(),
                            existing.emailVerified()
                    );
                    return keyCloakClient.updateUser(userId, request);
                })
                .map(KeycloakUserSummary::toDomain)
                .map(Account::fromKeycloak)
                .doOnSuccess(account -> log.info("Profile updated for userId: {}", userId))
                .doOnError(err -> log.warn("Error updating profile for userId: {}", userId));
    }

    @Override
    public Mono<Void> requestPasswordReset(String userId) {
        return keyCloakClient.sendUserActionsEmail(userId, List.of("UPDATE_PASSWORD"))
                .doOnSuccess(v -> log.info("Password reset email sent for userId: {}", userId))
                .doOnError(err -> log.warn("Error sending password reset email for userId: {}", userId));
    }

    @Override
    public Mono<Void> deleteAccount(String userId) {
        return keyCloakClient.deleteUser(userId)
                .doOnSuccess(v -> log.info("Account deleted for userId: {}", userId))
                .doOnError(err -> log.warn("Error deleting account for userId: {}", userId));
    }
}
