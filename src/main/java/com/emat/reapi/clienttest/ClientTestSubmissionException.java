package com.emat.reapi.clienttest;

import org.springframework.http.HttpStatus;

public class ClientTestSubmissionException extends RuntimeException {
    private final HttpStatus status;

    public ClientTestSubmissionException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public ClientTestSubmissionException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
