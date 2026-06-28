package com.emat.reapi.api.dto;

import jakarta.validation.constraints.*;

public record SubmissionUpdateDto(
        @NotBlank(message = "clientName is required")
        @Size(max = 200, message = "clientName must be at most 200 characters long")
        String clientName,

        @Email(message = "clientEmail must be a valid email address")
        @Size(max = 320, message = "clientEmail must be at most 320 characters long")
        String clientEmail,

        @NotBlank(message = "testId is required")
        @Size(max = 200, message = "testId must be at most 200 characters long")
        String testId,

        @Min(value = 1, message = "durationDays must be at least 1 days")
        @Max(value = 99, message = "durationDays cannot exceed 99 days")
        int durationDays
) {
}
