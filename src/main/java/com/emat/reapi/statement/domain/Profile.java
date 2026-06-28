package com.emat.reapi.statement.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Edytowalny profil (zastępuje sztywny enum {@link StatementProfile}).
 * Identyfikacja po niezmiennym {@code id}; labele/order edytowalne.
 * Progi etykiet (0/68) są globalne (config) — nie trzymamy ich tutaj.
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
