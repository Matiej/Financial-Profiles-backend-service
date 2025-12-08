package com.emat.reapi.profiler;

import org.springframework.http.HttpStatus;

public class ProfilerException  extends RuntimeException{
    private final ProfilerErrorType errorType;
    private final HttpStatus status;

    public ProfilerException(String message, HttpStatus status, ProfilerErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.status = status;
    }

    public ProfilerException(String message, HttpStatus status, Throwable cause, ProfilerErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
        this.status = status;
    }

    public ProfilerErrorType getErrorType() {
        return errorType;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public enum ProfilerErrorType {
        GENERATE_SCORING_PROFILE_ERROR
    }
}
