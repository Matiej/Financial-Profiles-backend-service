package com.emat.reapi.account.domain;

import com.emat.reapi.infrastructure.keycloak.KeycloakUserRequest;

public record UpdateProfileCommand(
        String firstName,
        String lastName,
        String email
) {

    public KeycloakUserRequest toKeycloakRequest(String existingUsername, boolean enabled, boolean emailVerified) {
        return new KeycloakUserRequest(
                existingUsername,
                this.firstName,
                this.lastName,
                this.email,
                enabled,
                emailVerified
        );
    }
}
