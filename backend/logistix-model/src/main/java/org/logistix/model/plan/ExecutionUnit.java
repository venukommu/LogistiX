package org.logistix.model.plan;

import org.logistix.model.node.DecisionNode;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Atomic executable unit compiled from a DecisionNode.
 */
public record ExecutionUnit(
        UUID unitId,
        DecisionNode node,
        Duration timeout,
        Map<String, Object> executionParameters
) {
    public ExecutionUnit {
        Objects.requireNonNull(unitId, "Unit ID must not be null");
        Objects.requireNonNull(node, "DecisionNode must not be null");
        timeout = timeout != null ? timeout : Duration.ofSeconds(5);
        executionParameters = executionParameters != null ? Map.copyOf(executionParameters) : Collections.emptyMap();
    }

    public static ExecutionUnit of(DecisionNode node) {
        return new ExecutionUnit(UUID.randomUUID(), node, Duration.ofSeconds(5), Collections.emptyMap());
    }
}
