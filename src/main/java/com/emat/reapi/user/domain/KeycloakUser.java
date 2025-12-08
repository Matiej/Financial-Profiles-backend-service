package com.emat.reapi.user.domain;

import java.time.Instant;

public record KeycloakUser(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt
) {
}
