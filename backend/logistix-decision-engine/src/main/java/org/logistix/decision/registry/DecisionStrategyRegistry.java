package org.logistix.decision.registry;

import org.logistix.domain.decision.DecisionStrategy;

import java.util.List;
import java.util.Optional;

/**
 * Registry for discovering and selecting decision strategies.
 */
public interface DecisionStrategyRegistry {

    void register(DecisionStrategy<?, ?> strategy);

    <C, R> Optional<DecisionStrategy<C, R>> getStrategy(String strategyName);

    List<DecisionStrategy<?, ?>> getAllStrategies();
}
