package com.emat.reapi.api.dto.statementdefiniton;

import com.emat.reapi.statement.domain.Profile;
import jakarta.validation.constraints.NotBlank;

/**
 * Create/update payload for a {@link Profile}. The client sends only editable fields:
 * the four label names and the display order. Server owns {@code id} (Mongo-generated),
 * {@code isDeleted} and timestamps.
 */
public record ProfileUpdateRequest(
        @NotBlank(message = "plName is required")
        String plName,

        @NotBlank(message = "blockingName is required")
        String blockingName,

        @NotBlank(message = "transitionalName is required")
        String transitionalName,

        @NotBlank(message = "resourcesName is required")
        String resourcesName,

        int order
) {
    /**
     * Maps the request to a domain profile carrying only the editable fields.
     * Identity, deletion flag and timestamps are assigned by the service.
     */
    public Profile toDomain() {
        return Profile.builder()
                .plName(plName)
                .blockingName(blockingName)
                .transitionalName(transitionalName)
                .resourcesName(resourcesName)
                .order(order)
                .build();
    }
}
