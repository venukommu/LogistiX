package org.logistix.api.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Standard error response payload conforming to RFC 7807 Problem Details concepts.
 */
@Schema(description = "Standardized error response payload")
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Application-specific error code", example = "VALIDATION_FAILED")
        String errorCode,

        @Schema(description = "Human-readable error description", example = "The request payload failed validation constraints.")
        String message,

        @Schema(description = "List of detailed field or constraint validation violations")
        List<String> details,

        @Schema(description = "Path of the request that produced the error", example = "/api/v1/dispatches")
        String path,

        @Schema(description = "Error timestamp", example = "2026-08-15T10:00:00Z")
        Instant timestamp
) {
    public ErrorResponse {
        details = details != null ? List.copyOf(details) : Collections.emptyList();
    }

    public static ErrorResponse of(int status, String errorCode, String message, String path) {
        return new ErrorResponse(status, errorCode, message, Collections.emptyList(), path, Instant.now());
    }

    public static ErrorResponse of(int status, String errorCode, String message, List<String> details, String path) {
        return new ErrorResponse(status, errorCode, message, details, path, Instant.now());
    }
}
