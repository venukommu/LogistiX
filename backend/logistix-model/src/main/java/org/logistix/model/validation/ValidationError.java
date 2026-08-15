package org.logistix.model.validation;

import java.util.Objects;

/**
 * Immutable diagnostic issue reported during model or configuration validation.
 */
public record ValidationError(
        String code,
        String message,
        String severity,
        String targetElementId
) {
    public ValidationError {
        Objects.requireNonNull(code, "Error code must not be null");
        Objects.requireNonNull(message, "Message must not be null");
        severity = severity != null ? severity : "ERROR";
        targetElementId = targetElementId != null ? targetElementId : "";
    }

    public static ValidationError error(String code, String message, String elementId) {
        return new ValidationError(code, message, "ERROR", elementId);
    }

    public static ValidationError warning(String code, String message, String elementId) {
        return new ValidationError(code, message, "WARNING", elementId);
    }
}
