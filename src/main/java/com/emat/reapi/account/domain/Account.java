package com.emat.reapi.account.domain;

import com.emat.reapi.infrastructure.keycloak.KeycloakUser;

import java.time.Instant;

public record Account(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt
) {

    public static Account fromKeycloak(KeycloakUser keycloakUser) {
        return new Account(
                keycloakUser.id(),
                keycloakUser.username(),
                keycloakUser.firstName(),
                keycloakUser.lastName(),
                keycloakUser.email(),
                keycloakUser.enabled(),
                keycloakUser.emailVerified(),
                keycloakUser.createdAt()
        );
    }
}
