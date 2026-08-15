package org.logistix.domain.decision;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable metadata characterizing a specific decision execution run.
 */
public record DecisionMetadata(
        UUID decisionId,
        String correlationId,
        String decisionType,
        String engineVersion,
        Instant requestedAt,
        ExecutionMode executionMode,
        Map<String, String> tags
) {
    public enum ExecutionMode {
        REAL_TIME,
        BATCH,
        SIMULATION,
        DRY_RUN,
        BENCHMARK
    }

    public DecisionMetadata {
        Objects.requireNonNull(decisionId, "Decision ID must not be null");
        Objects.requireNonNull(decisionType, "Decision type must not be null");
        Objects.requireNonNull(requestedAt, "RequestedAt timestamp must not be null");
        executionMode = executionMode != null ? executionMode : ExecutionMode.REAL_TIME;
        tags = tags != null ? Map.copyOf(tags) : Collections.emptyMap();
    }

    public static DecisionMetadata of(String decisionType) {
        return new DecisionMetadata(UUID.randomUUID(), UUID.randomUUID().toString(), decisionType, "0.1.0-SNAPSHOT", Instant.now(), ExecutionMode.REAL_TIME, Collections.emptyMap());
    }

    public static DecisionMetadata of(String decisionType, String correlationId) {
        return new DecisionMetadata(UUID.randomUUID(), correlationId, decisionType, "0.1.0-SNAPSHOT", Instant.now(), ExecutionMode.REAL_TIME, Collections.emptyMap());
    }
}
