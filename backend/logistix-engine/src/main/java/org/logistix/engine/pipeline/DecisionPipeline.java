package org.logistix.engine.pipeline;

import org.logistix.engine.builder.DecisionPipelineBuilder;
import org.logistix.engine.steps.DecisionStep;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable pipeline abstraction representing an ordered series of DecisionStep instances
 * configured for a specific decision type.
 */
public record DecisionPipeline(
        UUID pipelineId,
        String decisionType,
        String name,
        String version,
        List<DecisionStep> steps,
        Map<String, Object> metadata
) {
    public DecisionPipeline {
        Objects.requireNonNull(pipelineId, "Pipeline ID must not be null");
        Objects.requireNonNull(decisionType, "Decision type must not be null");
        Objects.requireNonNull(name, "Pipeline name must not be null");
        version = version != null ? version : "1.0.0";
        steps = steps != null ? List.copyOf(steps) : Collections.emptyList();
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public static DecisionPipelineBuilder builder() {
        return new DecisionPipelineBuilder();
    }

    public static DecisionPipelineBuilder builder(String decisionType) {
        return new DecisionPipelineBuilder().decisionType(decisionType);
    }
}
