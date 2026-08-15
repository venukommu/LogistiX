package org.logistix.engine.metrics;

import java.time.Duration;

/**
 * Collector contract for accumulating operational metrics during decision execution.
 */
public interface MetricsCollector {

    void recordStep(StepMetrics stepMetrics);

    void recordAiUsage(long tokens, Duration duration);

    void recordConfidence(double confidence);

    void recordWarning(String warning);

    void recordError(String error);

    DecisionMetrics snapshot(Duration totalDuration);
}
