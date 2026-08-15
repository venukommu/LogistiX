package org.logistix.engine.trace;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable replayable trace capturing the complete chronological step execution of a decision pipeline.
 */
public record DecisionTrace(
        UUID traceId,
        UUID contextId,
        String decisionType,
        List<DecisionTraceEntry> entries,
        Instant startedAt,
        Instant completedAt,
        Duration totalDuration
) {
    public DecisionTrace {
        Objects.requireNonNull(traceId, "Trace ID must not be null");
        Objects.requireNonNull(contextId, "Context ID must not be null");
        Objects.requireNonNull(decisionType, "Decision type must not be null");
        Objects.requireNonNull(startedAt, "StartedAt timestamp must not be null");
        entries = entries != null ? List.copyOf(entries) : Collections.emptyList();
        completedAt = completedAt != null ? completedAt : startedAt;
        totalDuration = totalDuration != null ? totalDuration : Duration.between(startedAt, completedAt);
    }
}
