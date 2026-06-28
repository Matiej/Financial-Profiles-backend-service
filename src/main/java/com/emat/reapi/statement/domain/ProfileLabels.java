package com.emat.reapi.statement.domain;

/**
 * Triple of scoring-zone labels for a single profile.
 * The source may be a live {@link Profile} (preview) or a snapshot stored in a test (history).
 */
public record ProfileLabels(
        String blocking,
        String transitional,
        String resources
) {
    public static ProfileLabels of(Profile profile) {
        return new ProfileLabels(
                profile.getBlockingName(),
                profile.getTransitionalName(),
                profile.getResourcesName()
        );
    }
}
