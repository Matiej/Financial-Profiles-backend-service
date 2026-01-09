package com.emat.reapi.api.dto.user;

import com.emat.reapi.user.domain.User;

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
    public static UserResponse fromDomain(User user) {
        return new UserResponse(
                user.id(),
                user.username(),
                user.firstName(),
                user.lastName(),
                user.email(),
                user.enabled(),
                user.emailVerified(),
                user.createdAt()
        );
    }
}
