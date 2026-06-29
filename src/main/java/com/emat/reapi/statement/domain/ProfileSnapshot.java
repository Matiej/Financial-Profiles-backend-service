package com.emat.reapi.statement.domain;

/**
 * Immutable copy of a {@link Profile}'s labels, frozen into a completed test
 * ({@code ClientTestDocument}) at save time. Scoring/AI/report read labels from
 * this snapshot so later edits/deletes of the live profile never change history.
 */
public record ProfileSnapshot(
        String id,
        String plName,
        String blockingName,
        String transitionalName,
        String resourcesName
) {
    public static ProfileSnapshot of(Profile profile) {
        return new ProfileSnapshot(
                profile.getId(),
                profile.getPlName(),
                profile.getBlockingName(),
                profile.getTransitionalName(),
                profile.getResourcesName()
        );
    }
}
