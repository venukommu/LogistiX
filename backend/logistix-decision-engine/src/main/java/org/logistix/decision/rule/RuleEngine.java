package org.logistix.decision.rule;

import org.logistix.decision.engine.DecisionContext;

import java.util.List;

/**
 * Engine contract for orchestrating composite rule sets.
 *
 * @param <T> Target candidate evaluation type
 */
public interface RuleEngine<T> {

    List<RuleEvaluationResult> evaluateAll(T candidate, DecisionContext<?> context);

    boolean passesAllHardConstraints(T candidate, DecisionContext<?> context);
}
