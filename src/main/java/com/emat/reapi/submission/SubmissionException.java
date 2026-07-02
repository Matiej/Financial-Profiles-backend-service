package com.emat.reapi.submission;

import lombok.Getter;

@Getter
public class SubmissionException extends RuntimeException {
    private final SubmissionErrorType type;

    public SubmissionException(String message, Throwable cause, SubmissionErrorType errorType) {
        super(message, cause);
        this.type = errorType;
    }

    public SubmissionException(String message, SubmissionErrorType errorType) {
        super(message);
        this.type = errorType;
    }

    public enum SubmissionErrorType {
        SUBMISSION_CREATE_ERROR,
        SUBMISSION_UPDATE_ERROR,
        SUBMISSION_NOT_FOUND,
        SUBMISSION_DELETE_ERROR,
        // target FpTest validation (create / extend a token)
        TEST_NOT_FOUND,
        TEST_DELETED
    }
}


