package org.logistix.decision.registry;

import org.logistix.domain.decision.DecisionEngine;

import java.util.List;
import java.util.Optional;

/**
 * Registry for discovering and dispatching decision engines by supported decision types.
 */
public interface DecisionEngineRegistry {

    void register(DecisionEngine<?, ?> engine);

    <C, R> Optional<DecisionEngine<C, R>> getEngine(String decisionType);

    List<DecisionEngine<?, ?>> getAllEngines();
}
