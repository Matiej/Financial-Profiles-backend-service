package com.emat.reapi.api.dto.account;

import com.emat.reapi.account.domain.UpdateProfileCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Field firstName can't be empty!")
        @Size(max = 255, message = "firstName is too long. Maximum size is 255 characters.")
        String firstName,

        @NotBlank(message = "Field lastName can't be empty!")
        @Size(max = 255, message = "lastName is too long. Maximum size is 255 characters.")
        String lastName,

        @NotBlank(message = "email is required")
        @Email(message = "Must be a valid email address")
        @Size(max = 320, message = "Email must be at most 320 characters long")
        String email
) {

    public UpdateProfileCommand toCommand() {
        return new UpdateProfileCommand(
                this.firstName,
                this.lastName,
                this.email
        );
    }
}
