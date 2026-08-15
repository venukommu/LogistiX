package org.logistix.decision.engine;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable contextual environment wrapper for decision intelligence workflows.
 *
 * @param <T> The target domain subject (e.g. Shipment, Route)
 */
public record DecisionContext<T>(
        T subject,
        Map<String, Object> environmentAttributes,
        Instant timestamp
) {
    public DecisionContext {
        Objects.requireNonNull(subject, "Decision context subject cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        environmentAttributes = environmentAttributes != null ? Map.copyOf(environmentAttributes) : Collections.emptyMap();
    }

    public static <T> DecisionContext<T> of(T subject) {
        return new DecisionContext<>(subject, Collections.emptyMap(), Instant.now());
    }

    public static <T> DecisionContext<T> of(T subject, Map<String, Object> attributes) {
        return new DecisionContext<>(subject, attributes, Instant.now());
    }
}
