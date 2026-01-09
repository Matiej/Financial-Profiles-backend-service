package com.emat.reapi.user.domain;

import com.emat.reapi.infrastructure.keycloak.KeycloakUserRequest;

public record CreateUserRequest(
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean emailVerified
) {

    public KeycloakUserRequest toKeycloakRequest() {
        return new KeycloakUserRequest(
                this.username,
                this.firstName,
                this.lastName,
                this.email,
                this.enabled,
                this.emailVerified
        );
    }
}
