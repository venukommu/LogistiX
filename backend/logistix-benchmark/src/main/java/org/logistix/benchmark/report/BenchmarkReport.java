package org.logistix.benchmark.report;

import org.logistix.benchmark.metric.BenchmarkMetric;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable report synthesizing benchmark metrics for an evaluation subject.
 */
public record BenchmarkReport(
        UUID reportId,
        String benchmarkSuiteName,
        String targetComponent,
        Instant executedAt,
        Duration totalDuration,
        List<BenchmarkMetric> metrics,
        Map<String, String> configurationSummary
) {
    public BenchmarkReport {
        Objects.requireNonNull(reportId, "Report ID must not be null");
        Objects.requireNonNull(benchmarkSuiteName, "Suite name must not be null");
        Objects.requireNonNull(targetComponent, "Target component must not be null");
        metrics = metrics != null ? List.copyOf(metrics) : Collections.emptyList();
        configurationSummary = configurationSummary != null ? Map.copyOf(configurationSummary) : Collections.emptyMap();
    }
}
