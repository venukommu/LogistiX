package org.logistix.domain.fact;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable atomic unit of domain information presented to the decision pipeline.
 *
 * @param <T> Payload value type
 */
public record Fact<T>(
        String key,
        T value,
        Class<T> type,
        FactSource source,
        double confidence,
        Instant timestamp
) {
    public Fact {
        Objects.requireNonNull(key, "Fact key must not be null");
        Objects.requireNonNull(value, "Fact value must not be null");
        Objects.requireNonNull(source, "Fact source must not be null");
        Objects.requireNonNull(timestamp, "Fact timestamp must not be null");
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        if (type == null) {
            @SuppressWarnings("unchecked")
            Class<T> inferredType = (Class<T>) value.getClass();
            type = inferredType;
        }
    }

    public static <T> Fact<T> of(String key, T value) {
        return new Fact<>(key, value, null, FactSource.SYSTEM, 1.0, Instant.now());
    }

    public static <T> Fact<T> of(String key, T value, FactSource source) {
        return new Fact<>(key, value, null, source, 1.0, Instant.now());
    }

    public static <T> Fact<T> of(String key, T value, FactSource source, double confidence) {
        return new Fact<>(key, value, null, source, confidence, Instant.now());
    }
}
