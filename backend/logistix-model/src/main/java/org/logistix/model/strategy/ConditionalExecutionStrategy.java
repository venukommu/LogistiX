package org.logistix.model.strategy;

/**
 * Strategy supporting dynamic runtime edge evaluation and condition branching.
 */
public interface ConditionalExecutionStrategy extends ExecutionStrategy {

    @Override
    default StrategyType getStrategyType() {
        return StrategyType.CONDITIONAL;
    }
}
