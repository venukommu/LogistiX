package org.logistix.examples;

import org.logistix.domain.constraint.Constraint;
import org.logistix.domain.constraint.ConstraintSeverity;
import org.logistix.domain.constraint.ConstraintViolation;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.dsl.annotation.DecisionConstraint;

import java.util.Optional;

/**
 * <h3>Custom Constraint Example</h3>
 * Demonstrates defining a hard feasibility guardrail constraint.
 */
@DecisionConstraint(id = "CONST-MAX-TRANSIT-HOURS", name = "Maximum Transit Duration Guardrail", severity = ConstraintSeverity.HARD)
public class CustomConstraintExample implements Constraint<CustomConstraintExample.RouteOption> {

    public record RouteOption(String routeId, double estimatedHours, double maxAllowedHours) {}

    @Override
    public String getId() {
        return "CONST-MAX-TRANSIT-HOURS";
    }

    @Override
    public String getName() {
        return "Maximum Transit Duration Guardrail";
    }

    @Override
    public ConstraintSeverity getSeverity() {
        return ConstraintSeverity.HARD;
    }

    @Override
    public Optional<ConstraintViolation> evaluate(RouteOption route, DecisionContext context) {
        if (route.estimatedHours() > route.maxAllowedHours()) {
            return Optional.of(ConstraintViolation.hard(
                    "CONST-MAX-TRANSIT-HOURS",
                    "Maximum Transit Duration Guardrail",
                    String.format("Estimated transit (%.1f hrs) exceeds maximum allowable window (%.1f hrs)",
                            route.estimatedHours(), route.maxAllowedHours())
            ));
        }
        return Optional.empty();
    }
}
