package org.logistix.model.strategy;

/**
 * Strategy supporting autonomous multi-agent reasoning, self-reflection loops, and dynamic tool orchestration.
 */
public interface AgentExecutionStrategy extends ExecutionStrategy {

    @Override
    default StrategyType getStrategyType() {
        return StrategyType.AGENT;
    }
}
