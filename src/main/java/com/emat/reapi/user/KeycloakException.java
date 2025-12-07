package com.emat.reapi.user;

import org.springframework.http.HttpStatus;

public class KeycloakException extends RuntimeException  {
    private final KeycloakException.KeyCloakExceptionErrorType type;
    private final HttpStatus status;

    public KeycloakException(String message, KeyCloakExceptionErrorType type, HttpStatus status) {
        super(message);
        this.type = type;
        this.status = status;
    }

    public KeycloakException(String message, Throwable cause, KeyCloakExceptionErrorType type, HttpStatus status) {
        super(message, cause);
        this.type = type;
        this.status = status;
    }

    public KeyCloakExceptionErrorType getType() {
        return type;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public enum KeyCloakExceptionErrorType{
        LIST_CALCULATOR_USER_ERROR,
        CREATE_CALCULATOR_USER_ERROR,
        USER_ALREADY_EXISTS,
        GENERIC_ERROR
    }
}
