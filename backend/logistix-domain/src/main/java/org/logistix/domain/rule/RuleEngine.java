package org.logistix.domain.rule;

import org.logistix.domain.decision.DecisionContext;

import java.util.List;

/**
 * Engine contract for orchestrating, prioritizing, and evaluating rule sets.
 *
 * @param <T> Candidate evaluation type
 */
public interface RuleEngine<T> {

    List<RuleOutcome> evaluate(T candidate, DecisionContext context);

    List<RuleOutcome> evaluateAll(List<T> candidates, DecisionContext context);
}
