package org.logistix.model.strategy;

/**
 * Strategy performing topological DAG sorting and dependency resolution across complex graph networks.
 */
public interface GraphExecutionStrategy extends ExecutionStrategy {

    @Override
    default StrategyType getStrategyType() {
        return StrategyType.GRAPH;
    }
}
