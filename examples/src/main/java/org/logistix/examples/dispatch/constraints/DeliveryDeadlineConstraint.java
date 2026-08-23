package org.logistix.examples.dispatch.constraints;

import org.logistix.domain.constraint.Constraint;
import org.logistix.domain.constraint.ConstraintSeverity;
import org.logistix.domain.constraint.ConstraintViolation;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Hard constraint verifying that estimated arrival at destination occurs on or before the strict SLA delivery deadline.
 */
public class DeliveryDeadlineConstraint implements Constraint<DispatchCandidate> {

    public static final String ID = "CONSTRAINT_DELIVERY_DEADLINE";
    public static final String NAME = "Delivery Deadline Feasibility";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ConstraintSeverity getSeverity() {
        return ConstraintSeverity.HARD;
    }

    @Override
    public Optional<ConstraintViolation> evaluate(DispatchCandidate candidate, DecisionContext context) {
        Instant eta = candidate.estimatedDeliveryTime();
        Instant deadline = candidate.shipment().deliveryDeadline();

        if (eta.isAfter(deadline)) {
            long delayMinutes = Duration.between(deadline, eta).toMinutes();
            return Optional.of(new ConstraintViolation(
                    ID,
                    NAME,
                    ConstraintSeverity.HARD,
                    String.format("Estimated delivery time (%s) misses deadline (%s) by %d minutes",
                            eta, deadline, delayMinutes),
                    Map.of(
                            "estimatedDeliveryTime", eta.toString(),
                            "deliveryDeadline", deadline.toString(),
                            "delayMinutes", delayMinutes
                    ),
                    context.timestamp()
            ));
        }

        return Optional.empty();
    }
}
