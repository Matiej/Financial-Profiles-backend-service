package com.emat.reapi.user.domain;

import com.emat.reapi.infrastructure.keycloak.KeycloakUser;

import java.time.Instant;

public record User(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt
) {

    public static User fromKeycloak(KeycloakUser keycloakUser) {
        return new User(
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
