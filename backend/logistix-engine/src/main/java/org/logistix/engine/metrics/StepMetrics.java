package org.logistix.engine.metrics;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable metrics measurement for an individual pipeline execution step.
 */
public record StepMetrics(
        String stepId,
        String stepName,
        Duration duration,
        int factsEmitted,
        int rulesEvaluated,
        int constraintsEvaluated,
        boolean success
) {
    public StepMetrics {
        Objects.requireNonNull(stepId, "Step ID must not be null");
        Objects.requireNonNull(stepName, "Step name must not be null");
        Objects.requireNonNull(duration, "Duration must not be null");
    }

    public static StepMetrics of(String stepId, String stepName, Duration duration, boolean success) {
        return new StepMetrics(stepId, stepName, duration, 0, 0, 0, success);
    }
}
