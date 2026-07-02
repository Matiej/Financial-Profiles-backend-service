package com.emat.reapi.statement;

/**
 * Signals an illegal statement-definition lifecycle transition (e.g. deleting a definition
 * still referenced by an existing FpTest, or reassigning the profile of a definition used
 * in an already-submitted test). Mapped to HTTP 409 in the global exception handler.
 */
public class StatementDefinitionStateException extends RuntimeException {
    private final DefinitionErrorType type;

    public StatementDefinitionStateException(String message, DefinitionErrorType type) {
        super(message);
        this.type = type;
    }

    public DefinitionErrorType getType() {
        return type;
    }

    public enum DefinitionErrorType {
        DEFINITION_IN_USE,
        DEFINITION_EDIT_ERROR
    }
}
