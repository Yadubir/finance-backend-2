package com.finance.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handling for all controllers.
 *
 * Every error returns the same envelope shape:
 * {
 *   "status":    404,
 *   "error":     "Not Found",
 *   "message":   "FinancialRecord not found with id: 99",
 *   "timestamp": "2024-03-15T10:30:00"
 * }
 *
 * This consistency makes frontend error handling straightforward.
 *
 * Tradeoff: A global handler catches everything — convenient but can
 * accidentally swallow errors you want to surface differently. Prefer
 * explicit exception types over generic catch-alls.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── Standard error envelope

    @Data @Builder @AllArgsConstructor
    public static class ErrorResponse {
        private int           status;
        private String        error;
        private String        message;
        private LocalDateTime timestamp;
    }

    @Data @Builder @AllArgsConstructor
    public static class ValidationErrorResponse {
        private int                 status;
        private String              error;
        private String              message;
        private Map<String, String> fieldErrors;
        private LocalDateTime       timestamp;
    }

    // ── Handler methods

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Handles @PreAuthorize failures — returns 403 instead of Spring's default redirect. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }

    /** Invalid credentials during login. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        // Intentionally vague — don't tell the caller whether email or password was wrong
        return error(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    /** Inactive / soft-deleted user attempts to log in. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Account is inactive. Please contact an administrator.");
    }

    /**
     * Bean Validation failures from @Valid on request bodies.
     * Returns all field errors so the client can highlight every invalid field at once.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err -> {
            String field   = err instanceof FieldError fe ? fe.getField() : err.getObjectName();
            String message = err.getDefaultMessage();
            fieldErrors.put(field, message);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ValidationErrorResponse.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Validation Failed")
                        .message("One or more fields are invalid")
                        .fieldErrors(fieldErrors)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    /** Wrong type for a path/query parameter (e.g. "abc" for a Long id). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' must be of type %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /** Catch-all for unexpected errors — logs the full stack trace. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    // Helper

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
