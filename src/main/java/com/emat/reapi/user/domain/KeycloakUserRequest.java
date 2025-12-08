package com.emat.reapi.user.domain;

public record KeycloakUserRequest(
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean emailVerified
) {
}
