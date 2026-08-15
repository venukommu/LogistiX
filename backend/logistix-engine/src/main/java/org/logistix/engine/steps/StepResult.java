package org.logistix.engine.steps;

import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable outcome produced by the execution of a DecisionStep.
 */
public record StepResult(
        DecisionContext context,
        StepStatus status,
        String message,
        Duration duration,
        List<Fact<?>> emittedFacts
) {
    public StepResult {
        Objects.requireNonNull(context, "DecisionContext must not be null");
        Objects.requireNonNull(status, "StepStatus must not be null");
        duration = duration != null ? duration : Duration.ZERO;
        emittedFacts = emittedFacts != null ? List.copyOf(emittedFacts) : Collections.emptyList();
    }

    public static StepResult success(DecisionContext context, Duration duration, String message) {
        return new StepResult(context, StepStatus.SUCCESS, message, duration, Collections.emptyList());
    }

    public static StepResult success(DecisionContext context, Duration duration, List<Fact<?>> emittedFacts, String message) {
        return new StepResult(context, StepStatus.SUCCESS, message, duration, emittedFacts);
    }

    public static StepResult skipped(DecisionContext context, String reason) {
        return new StepResult(context, StepStatus.SKIPPED, reason, Duration.ZERO, Collections.emptyList());
    }

    public static StepResult failed(DecisionContext context, Duration duration, String errorMessage) {
        return new StepResult(context, StepStatus.FAILED, errorMessage, duration, Collections.emptyList());
    }

    public static StepResult shortCircuit(DecisionContext context, Duration duration, String reason) {
        return new StepResult(context, StepStatus.SHORT_CIRCUIT, reason, duration, Collections.emptyList());
    }
}
