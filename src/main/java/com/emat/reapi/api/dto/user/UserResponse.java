package com.emat.reapi.api.dto.user;

import com.emat.reapi.user.domain.KeycloakUser;

import java.time.Instant;

public record UserResponse(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt
) {
    public static UserResponse fromDomain(KeycloakUser keycloakUser) {
        return new UserResponse(
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
