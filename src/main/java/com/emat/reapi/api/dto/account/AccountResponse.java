package com.emat.reapi.api.dto.account;

import com.emat.reapi.account.domain.Account;

import java.time.Instant;

public record AccountResponse(
        String id,
        String username,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt
) {

    public static AccountResponse fromDomain(Account account) {
        return new AccountResponse(
                account.id(),
                account.username(),
                account.firstName(),
                account.lastName(),
                account.email(),
                account.enabled(),
                account.emailVerified(),
                account.createdAt()
        );
    }
}
