package org.logistix.model.plan;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Tracks runtime progress, completed stages, and failure locations within an ExecutionPlan.
 */
public record ExecutionCursor(
        int currentStageIndex,
        int currentUnitIndex,
        List<String> completedNodeIds,
        List<String> failedNodeIds,
        boolean completed
) {
    public ExecutionCursor {
        completedNodeIds = completedNodeIds != null ? List.copyOf(completedNodeIds) : Collections.emptyList();
        failedNodeIds = failedNodeIds != null ? List.copyOf(failedNodeIds) : Collections.emptyList();
    }

    public static ExecutionCursor initial() {
        return new ExecutionCursor(0, 0, Collections.emptyList(), Collections.emptyList(), false);
    }
}
