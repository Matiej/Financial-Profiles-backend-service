package com.emat.reapi.user.infra;

import com.emat.reapi.user.domain.KeycloakUser;

import java.time.Instant;

public record KeycloakUserSummary(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt
) {

    public KeycloakUser toDomain() {
        return new KeycloakUser(
                this.id,
                this.username,
                this.firstName,
                this.lastName,
                this.email,
                this.enabled,
                this.emailVerified,
                this.createdAt
        );
    }
}
