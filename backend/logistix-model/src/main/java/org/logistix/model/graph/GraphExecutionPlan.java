package org.logistix.model.graph;

import org.logistix.model.plan.ExecutionPlan;
import org.logistix.model.plan.ExecutionStage;
import org.logistix.model.strategy.StrategyType;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Specialized ExecutionPlan compiled specifically from a topological DAG DecisionGraph.
 */
public record GraphExecutionPlan(
        ExecutionPlan plan,
        boolean containsCycles,
        int maximumParallelism
) {
    public static GraphExecutionPlan of(String modelId, List<ExecutionStage> stages, int maxParallelism) {
        ExecutionPlan basePlan = new ExecutionPlan(
                UUID.randomUUID(),
                modelId,
                StrategyType.GRAPH.name(),
                stages,
                Collections.emptyMap()
        );
        return new GraphExecutionPlan(basePlan, false, maxParallelism);
    }
}
