package org.logistix.domain.constraint;

import org.logistix.domain.decision.DecisionContext;

import java.util.List;

/**
 * Engine contract for enforcing boundary conditions and hard pruning in the decision flow.
 *
 * @param <T> Candidate type
 */
public interface ConstraintEngine<T> {

    List<ConstraintViolation> validate(T candidate, DecisionContext context);

    boolean isFeasible(T candidate, DecisionContext context);

    List<T> filterFeasible(List<T> candidates, DecisionContext context);
}
