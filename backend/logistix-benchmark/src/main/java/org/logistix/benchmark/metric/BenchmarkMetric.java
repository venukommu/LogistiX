package org.logistix.benchmark.metric;

import java.util.Objects;

/**
 * Immutable quantitative measurement reported during an evaluation run.
 */
public record BenchmarkMetric(
        String name,
        double value,
        String unit,
        String category,
        String description
) {
    public BenchmarkMetric {
        Objects.requireNonNull(name, "Metric name must not be null");
        Objects.requireNonNull(unit, "Unit must not be null");
        Objects.requireNonNull(category, "Category must not be null");
    }

    public static BenchmarkMetric of(String name, double value, String unit, String category) {
        return new BenchmarkMetric(name, value, unit, category, null);
    }
}
