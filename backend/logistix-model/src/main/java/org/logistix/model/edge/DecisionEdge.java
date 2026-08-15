package org.logistix.model.edge;

import java.util.Objects;
import java.util.UUID;

/**
 * Directed edge representing dependencies, control flow, or conditional routing between DecisionNodes.
 */
public record DecisionEdge(
        UUID edgeId,
        String sourceNodeId,
        String targetNodeId,
        EdgeType type,
        String conditionExpression,
        double weight
) {
    public DecisionEdge {
        Objects.requireNonNull(edgeId, "Edge ID must not be null");
        Objects.requireNonNull(sourceNodeId, "Source node ID must not be null");
        Objects.requireNonNull(targetNodeId, "Target node ID must not be null");
        Objects.requireNonNull(type, "Edge type must not be null");
        conditionExpression = conditionExpression != null ? conditionExpression : "";
    }

    public static DecisionEdge dependsOn(String sourceNodeId, String targetNodeId) {
        return new DecisionEdge(UUID.randomUUID(), sourceNodeId, targetNodeId, EdgeType.DEPENDS_ON, "", 1.0);
    }

    public static DecisionEdge sequence(String sourceNodeId, String targetNodeId) {
        return new DecisionEdge(UUID.randomUUID(), sourceNodeId, targetNodeId, EdgeType.RUNS_AFTER, "", 1.0);
    }

    public static DecisionEdge conditional(String sourceNodeId, String targetNodeId, String expression) {
        return new DecisionEdge(UUID.randomUUID(), sourceNodeId, targetNodeId, EdgeType.CONDITIONAL, expression, 1.0);
    }
}
