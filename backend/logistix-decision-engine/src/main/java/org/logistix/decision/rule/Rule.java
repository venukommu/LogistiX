package org.logistix.decision.rule;

import org.logistix.decision.engine.DecisionContext;

/**
 * Domain or business rule interface.
 *
 * @param <T> Target evaluation type
 */
public interface Rule<T> {

    String getRuleId();

    String getName();

    int getPriority();

    RuleEvaluationResult evaluate(T candidate, DecisionContext<?> context);
}
