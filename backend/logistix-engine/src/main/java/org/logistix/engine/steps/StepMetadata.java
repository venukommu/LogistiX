package org.logistix.engine.steps;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable descriptor characterizing a pipeline step.
 */
public record StepMetadata(
        String stepId,
        String name,
        int order,
        boolean optional,
        Duration timeout,
        Map<String, Object> attributes
) {
    public StepMetadata {
        Objects.requireNonNull(stepId, "Step ID must not be null");
        Objects.requireNonNull(name, "Step name must not be null");
        timeout = timeout != null ? timeout : Duration.ofSeconds(5);
        attributes = attributes != null ? Map.copyOf(attributes) : Collections.emptyMap();
    }

    public static StepMetadata of(String stepId, String name, int order) {
        return new StepMetadata(stepId, name, order, false, Duration.ofSeconds(5), Collections.emptyMap());
    }

    public static StepMetadata optional(String stepId, String name, int order) {
        return new StepMetadata(stepId, name, order, true, Duration.ofSeconds(5), Collections.emptyMap());
    }
}
