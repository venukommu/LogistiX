package org.logistix.engine.trace;

import org.logistix.engine.steps.StepStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable replayable trace entry recording the discrete state change caused by an individual step.
 */
public record DecisionTraceEntry(
        UUID entryId,
        String stepId,
        String stepName,
        StepStatus status,
        Duration duration,
        Instant timestamp,
        String message,
        List<String> emittedFactKeys,
        Map<String, Object> stateAttributes
) {
    public DecisionTraceEntry {
        Objects.requireNonNull(entryId, "Entry ID must not be null");
        Objects.requireNonNull(stepId, "Step ID must not be null");
        Objects.requireNonNull(stepName, "Step name must not be null");
        Objects.requireNonNull(status, "Status must not be null");
        Objects.requireNonNull(duration, "Duration must not be null");
        Objects.requireNonNull(timestamp, "Timestamp must not be null");
        emittedFactKeys = emittedFactKeys != null ? List.copyOf(emittedFactKeys) : Collections.emptyList();
        stateAttributes = stateAttributes != null ? Map.copyOf(stateAttributes) : Collections.emptyMap();
    }

    public static DecisionTraceEntry of(String stepId, String stepName, StepStatus status, Duration duration, String message) {
        return new DecisionTraceEntry(UUID.randomUUID(), stepId, stepName, status, duration, Instant.now(), message, Collections.emptyList(), Collections.emptyMap());
    }
}
