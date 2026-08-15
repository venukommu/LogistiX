package org.logistix.domain.ports;

import org.logistix.domain.constraint.Constraint;
import org.logistix.domain.decision.DecisionContext;

import java.util.List;

/**
 * Outbound SPI for dynamically loading operational feasibility constraints for a decision context.
 */
public interface ConstraintProvider {

    <T> List<Constraint<T>> getConstraintsForContext(DecisionContext context, Class<T> candidateType);
}
