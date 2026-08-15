package org.logistix.model.strategy;

/**
 * Strategy compiling models into strictly sequential, single-threaded execution stages.
 */
public interface SequentialExecutionStrategy extends ExecutionStrategy {

    @Override
    default StrategyType getStrategyType() {
        return StrategyType.SEQUENTIAL;
    }
}
