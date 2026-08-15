package org.logistix.domain.constraint;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable record representing an explicit constraint violation.
 */
public record ConstraintViolation(
        String constraintId,
        String constraintName,
        ConstraintSeverity severity,
        String message,
        Map<String, Object> attributes,
        Instant timestamp
) {
    public ConstraintViolation {
        Objects.requireNonNull(constraintId, "Constraint ID must not be null");
        Objects.requireNonNull(constraintName, "Constraint name must not be null");
        Objects.requireNonNull(severity, "Severity must not be null");
        Objects.requireNonNull(message, "Message must not be null");
        attributes = attributes != null ? Map.copyOf(attributes) : Collections.emptyMap();
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static ConstraintViolation hard(String constraintId, String constraintName, String message) {
        return new ConstraintViolation(constraintId, constraintName, ConstraintSeverity.HARD, message, Collections.emptyMap(), Instant.now());
    }

    public static ConstraintViolation soft(String constraintId, String constraintName, String message) {
        return new ConstraintViolation(constraintId, constraintName, ConstraintSeverity.SOFT, message, Collections.emptyMap(), Instant.now());
    }
}
