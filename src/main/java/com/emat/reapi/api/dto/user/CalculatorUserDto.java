package com.emat.reapi.api.dto.user;

import com.emat.reapi.user.domain.KeycloakUserRequest;

public record CalculatorUserDto(
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean emailVerified
) {

    public KeycloakUserRequest toRequest() {
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
