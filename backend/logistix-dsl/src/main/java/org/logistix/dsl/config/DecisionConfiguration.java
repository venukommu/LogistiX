package org.logistix.dsl.config;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable configuration parameters for decision engine executions.
 */
public record DecisionConfiguration(
        Duration defaultTimeout,
        boolean strictConstraints,
        boolean enableExplainability,
        double minAcceptanceScore,
        Map<String, Object> parameters
) {
    public DecisionConfiguration {
        defaultTimeout = defaultTimeout != null ? defaultTimeout : Duration.ofSeconds(10);
        parameters = parameters != null ? Map.copyOf(parameters) : Collections.emptyMap();
    }

    public static DecisionConfiguration defaults() {
        return new DecisionConfiguration(Duration.ofSeconds(10), true, true, 0.60, Collections.emptyMap());
    }
}
