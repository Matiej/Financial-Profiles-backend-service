package com.emat.reapi.statement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Editable profile (replaced the former fixed StatementProfile enum).
 * Identified by an immutable {@code id}; labels/order are editable.
 * Label thresholds (0/68) are global (config) — not stored here.
 */
@Data
@Builder
@AllArgsConstructor
public class Profile {
    private String id;
    private String plName;
    private String blockingName;
    private String transitionalName;
    private String resourcesName;
    private int order;
    private boolean isDeleted;
    private Instant createdAt;
    private Instant updatedAt;
}
