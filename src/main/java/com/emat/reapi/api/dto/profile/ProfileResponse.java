package com.emat.reapi.api.dto.profile;

import com.emat.reapi.statement.domain.Profile;

import java.time.Instant;

/**
 * Full profile view returned to the admin panel.
 */
public record ProfileResponse(
        String id,
        String plName,
        String blockingName,
        String transitionalName,
        String resourcesName,
        int order,
        boolean isDeleted,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProfileResponse toResponse(Profile domain) {
        return new ProfileResponse(
                domain.getId(),
                domain.getPlName(),
                domain.getBlockingName(),
                domain.getTransitionalName(),
                domain.getResourcesName(),
                domain.getOrder(),
                domain.isDeleted(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
