package org.logistix.domain.constraint;

import org.logistix.domain.decision.DecisionContext;

import java.util.Optional;

/**
 * Domain boundary condition or operational feasibility guardrail.
 *
 * @param <T> Target candidate evaluation type
 */
public interface Constraint<T> {

    String getId();

    String getName();

    ConstraintSeverity getSeverity();

    Optional<ConstraintViolation> evaluate(T candidate, DecisionContext context);
}
