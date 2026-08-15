package org.logistix.domain.rule;

import org.logistix.domain.decision.DecisionContext;

/**
 * Domain business rule contract defining deterministic operational logic.
 *
 * @param <T> Candidate evaluation type
 */
public interface Rule<T> {

    String getId();

    String getName();

    int getPriority();

    RuleOutcome evaluate(T candidate, DecisionContext context);
}
