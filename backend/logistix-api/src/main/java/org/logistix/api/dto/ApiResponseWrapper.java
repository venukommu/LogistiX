package org.logistix.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standard API response envelope.
 *
 * @param <T> Payload body type
 */
@Schema(description = "Standard API response wrapper envelope")
public record ApiResponseWrapper<T>(
        @Schema(description = "Execution status flag", example = "true")
        boolean success,

        @Schema(description = "Response payload body")
        T data,

        @Schema(description = "Response creation timestamp", example = "2026-08-15T10:00:00Z")
        Instant timestamp
) {
    public static <T> ApiResponseWrapper<T> ok(T data) {
        return new ApiResponseWrapper<>(true, data, Instant.now());
    }
}
