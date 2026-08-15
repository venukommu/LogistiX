package org.logistix.api.v1.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Standard envelope wrapping API response payloads.
 *
 * @param <T> Payload body type
 */
@Schema(description = "Standard API response wrapper")
public record ApiResponseWrapper<T>(
        @Schema(description = "Status flag", example = "true")
        boolean success,

        @Schema(description = "Response payload body")
        T data,

        @Schema(description = "Response timestamp", example = "2026-08-15T10:00:00Z")
        Instant timestamp
) {
    public static <T> ApiResponseWrapper<T> ok(T data) {
        return new ApiResponseWrapper<>(true, data, Instant.now());
    }
}
