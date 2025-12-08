package com.emat.reapi.global.error;

import com.emat.reapi.clienttest.ClientTestException;
import com.emat.reapi.fptest.FpTestStateException;
import com.emat.reapi.profiler.ProfilerException;
import com.emat.reapi.submission.SubmissionException;
import com.emat.reapi.submission.SubmissionStateException;
import com.emat.reapi.user.KeycloakException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(KeycloakException.class)
    public ResponseEntity<ErrorResponse> keyCloakException(KeycloakException ex, ServerHttpRequest request) {
        log.warn("SubmissionException: {}", ex.getMessage(), ex);
        HttpStatus status = ex.getStatus();
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getType().name(),
                ex.getMessage(),
                request.getPath().value(),
                null
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(ProfilerException.class)
    public ResponseEntity<ErrorResponse> clientProfilerException(ProfilerException ex, ServerHttpRequest request) {
        log.warn("SubmissionException: {}", ex.getMessage(), ex);
        HttpStatus status = ex.getStatus();
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getErrorType().name(),
                ex.getMessage(),
                request.getPath().value(),
                null
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(ClientTestException.class)
    public ResponseEntity<ErrorResponse> clientTEstException(ClientTestException ex, ServerHttpRequest request) {
        log.warn("SubmissionException: {}", ex.getMessage(), ex);
        HttpStatus status = ex.getStatus();
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "CLIENT_TEST_ERROR",
                ex.getMessage(),
                request.getPath().value(),
                null
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(SubmissionException.class)
    public ResponseEntity<ErrorResponse> handleSubmissionException(SubmissionException ex, ServerHttpRequest request) {
        log.warn("SubmissionException: {}", ex.getMessage(), ex);
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getType().name(),
                ex.getMessage(),
                request.getPath().value(),
                null
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler({FpTestStateException.class})
    public ResponseEntity<ErrorResponse> handleFpTestStateException(FpTestStateException ex, ServerHttpRequest request) {
        log.warn("FpTestStateException: {}", ex.getMessage(), ex);
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getType().name(),
                ex.getMessage(),
                request.getPath().value(),
                null
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler({SubmissionStateException.class})
    public ResponseEntity<ErrorResponse> handleSubmissionStateException(SubmissionStateException ex, ServerHttpRequest request) {
        log.warn("SubmissionStateException: {}", ex.getMessage(), ex);
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getType().name(),
                ex.getMessage(),
                request.getPath().value(),
                null
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, ServerHttpRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        log.warn("ResponseStatusException {} at {}: {}", status, request.getPath(), ex.getReason());
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "GENERIC_STATUS_ERROR",
                ex.getReason(),
                request.getPath().value(),
                null
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(WebExchangeBindException ex, ServerHttpRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        Map<String, Object> details = ex.getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> b
                ));
        log.warn("Validation error at {}: {}", request.getPath(), details);
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "VALIDATION_ERROR",
                "Validation failed",
                request.getPath().value(),
                details
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   ServerHttpRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        Map<String, Object> details = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> b
                ));

        log.warn("Constraint violation at {}: {}", request.getPath(), details);

        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "CONSTRAINT_VIOLATION",
                "Constraint violation",
                request.getPath().value(),
                details
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, ServerHttpRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        log.warn("IllegalArgument at {}: {}", request.getPath(), ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "ILLEGAL_ARGUMENT",
                ex.getMessage(),
                request.getPath().value(),
                null
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, ServerHttpRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.error("Unexpected error at {}: {}", request.getPath(), ex.getMessage(), ex);
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "INTERNAL_ERROR",
                "Unexpected server error",
                request.getPath().value(),
                null
        );
        return ResponseEntity.status(status).body(errorResponse);
    }

}
