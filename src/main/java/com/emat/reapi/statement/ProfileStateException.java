package com.emat.reapi.statement;

/**
 * Signals an illegal profile lifecycle transition (e.g. deleting a profile still
 * referenced by definitions). Mapped to HTTP 409 in the global exception handler.
 */
public class ProfileStateException extends RuntimeException {
    private final ProfileErrorType type;

    public ProfileStateException(String message, ProfileErrorType type) {
        super(message);
        this.type = type;
    }

    public ProfileErrorType getType() {
        return type;
    }

    public enum ProfileErrorType {
        PROFILE_IN_USE
    }
}
