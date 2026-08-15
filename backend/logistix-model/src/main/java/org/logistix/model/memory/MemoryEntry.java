package org.logistix.model.memory;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable memory record representing a recalled experience, past decision outcome,
 * or conversational context.
 */
public record MemoryEntry(
        String key,
        Object value,
        double relevance,
        Instant timestamp,
        Map<String, Object> metadata
) {
    public MemoryEntry {
        Objects.requireNonNull(key, "Memory key must not be null");
        Objects.requireNonNull(value, "Memory value must not be null");
        timestamp = timestamp != null ? timestamp : Instant.now();
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static MemoryEntry of(String key, Object value, double relevance) {
        return new MemoryEntry(key, value, relevance, Instant.now(), Collections.emptyMap());
    }
}
