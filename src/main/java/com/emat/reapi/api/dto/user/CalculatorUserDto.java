package com.emat.reapi.api.dto.user;

public record UpdateCalculatorUserDto(
        String id,
        String username,
        String email,
        boolean enabled,
        boolean emailVerified
) {
}
