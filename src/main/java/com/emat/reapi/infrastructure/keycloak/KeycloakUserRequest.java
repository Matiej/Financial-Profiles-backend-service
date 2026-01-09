package com.emat.reapi.infrastructure.keycloak;

public record KeycloakUserRequest(
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean emailVerified
) {
}
