package org.logistix.model.plan;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Compiled executable schedule derived from a DecisionModel via an ExecutionStrategy.
 */
public record ExecutionPlan(
        UUID planId,
        String modelId,
        String strategyType,
        List<ExecutionStage> stages,
        Map<String, Object> metadata
) {
    public ExecutionPlan {
        Objects.requireNonNull(planId, "Plan ID must not be null");
        Objects.requireNonNull(modelId, "Model ID must not be null");
        Objects.requireNonNull(strategyType, "Strategy type must not be null");
        stages = stages != null ? List.copyOf(stages) : Collections.emptyList();
        metadata = metadata != null ? Map.copyOf(metadata) : Collections.emptyMap();
    }

    public int totalUnits() {
        return stages.stream().mapToInt(s -> s.units().size()).sum();
    }
}
