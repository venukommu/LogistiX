package org.logistix.model.strategy;

/**
 * Strategy compiling independent nodes into concurrently executable stages.
 */
public interface ParallelExecutionStrategy extends ExecutionStrategy {

    @Override
    default StrategyType getStrategyType() {
        return StrategyType.PARALLEL;
    }
}
