package com.emat.reapi.api.dto.user;

import com.emat.reapi.user.domain.KeycloakUserRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CalculatorUserDto(
        @NotBlank(message = "Field username can't be empty!")
        @Size(max = 255, message = "To long username Maximum size of testId filed is 255 characters.")
        String username,

        @NotBlank(message = "Field firstName can't be empty!")
        @Size(max = 255, message = "To long firstName Maximum size of testId filed is 255 characters.")
        String firstName,

        @NotBlank(message = "Field lastName can't be empty!")
        @Size(max = 255, message = "To long lastName Maximum size of testId filed is 255 characters.")
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 320, message = "Email must be at most 320 characters long")
        String email,

        Boolean enabled,
        Boolean emailVerified
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
