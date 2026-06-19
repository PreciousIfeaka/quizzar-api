package com.quizzar.common.handler;

import com.quizzar.common.dto.ErrorResponse;
import com.quizzar.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(QuizNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleQuizNotFound(QuizNotFoundException ex, HttpServletRequest req) {
        log.error("Quiz not found: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(QuizOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleOwnership(QuizOwnershipException ex, HttpServletRequest req) {
        log.error("Forbidden Access: {}", ex.getMessage());
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileTypeException ex, HttpServletRequest req) {
        log.error("Invalid file type: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE", "Invalid file type", req);
    }

    @ExceptionHandler(FileSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSize(FileSizeExceededException ex, HttpServletRequest req) {
        log.error("File size is too large: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "FILE_TOO_LARGE", ex.getMessage(), req);
    }

    @ExceptionHandler(DocumentExtractionException.class)
    public ResponseEntity<ErrorResponse> handleExtraction(DocumentExtractionException ex, HttpServletRequest req) {
        log.error("Failed to extract text from file: {}", ex.getMessage());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "EXTRACTION_FAILED", "Text extraction failed", req);
    }

    @ExceptionHandler(AiGenerationException.class)
    public ResponseEntity<ErrorResponse> handleAi(AiGenerationException ex, HttpServletRequest req) {
        log.error("AI generation failed: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_GATEWAY, "AI_SERVICE_ERROR", "AI generation failed", req);
    }

    @ExceptionHandler(S3OperationException.class)
    public ResponseEntity<ErrorResponse> handleS3(S3OperationException ex, HttpServletRequest req) {
        log.error("S3 operation failed: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_GATEWAY, "STORAGE_ERROR", "File upload failed", req);
    }

    @ExceptionHandler(NoSuchBucketException.class)
    public  ResponseEntity<ErrorResponse> handleBucketNotFound(NoSuchBucketException ex, HttpServletRequest req) {
        log.error("S3 bucket not found: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_GATEWAY, "STORAGE_ERROR", "File upload failed", req);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex, HttpServletRequest req) {
        log.error("Too many requests: {}", ex.getMessage());
        return buildError(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied", req);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(org.springframework.security.core.AuthenticationException ex, HttpServletRequest req) {
        log.error("Authentication failed: {}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        log.error("Bad request: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        log.error("Unsupported media type exception {}", ex.getMessage(), ex);
        return buildError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String errorCode, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
            .success(false)
            .timestamp(OffsetDateTime.now().toString())
            .status(status.value())
            .error(errorCode)
            .message(message)
            .path(req.getRequestURI())
            .traceId(MDC.get("traceId"))
            .build());
    }
}
