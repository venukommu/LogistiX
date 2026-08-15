package org.logistix.engine.metrics;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable aggregated metrics profile collected across a decision pipeline execution.
 */
public record DecisionMetrics(
        Duration totalExecutionDuration,
        List<StepMetrics> stepMetrics,
        int evaluatedRuleCount,
        int passedRuleCount,
        int violatedConstraintCount,
        long aiTokensConsumed,
        Duration aiDuration,
        double confidenceScore,
        int warningCount,
        int errorCount
) {
    public DecisionMetrics {
        Objects.requireNonNull(totalExecutionDuration, "Total execution duration must not be null");
        stepMetrics = stepMetrics != null ? List.copyOf(stepMetrics) : Collections.emptyList();
        aiDuration = aiDuration != null ? aiDuration : Duration.ZERO;
    }

    public static DecisionMetrics empty() {
        return new DecisionMetrics(Duration.ZERO, Collections.emptyList(), 0, 0, 0, 0, Duration.ZERO, 0.0, 0, 0);
    }
}
