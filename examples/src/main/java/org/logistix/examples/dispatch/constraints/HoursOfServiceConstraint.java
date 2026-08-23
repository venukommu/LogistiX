package org.logistix.examples.dispatch.constraints;

import org.logistix.domain.constraint.Constraint;
import org.logistix.domain.constraint.ConstraintSeverity;
import org.logistix.domain.constraint.ConstraintViolation;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Hard constraint ensuring the candidate driver has sufficient legal Hours of Service (HOS)
 * to complete deadhead repositioning plus the linehaul transit.
 */
public class HoursOfServiceConstraint implements Constraint<DispatchCandidate> {

    public static final String ID = "CONSTRAINT_HOS";
    public static final String NAME = "Hours of Service Limit";

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
        Duration requiredTime = candidate.totalRequiredDrivingDuration();
        Duration availableHos = candidate.driver().remainingHos();

        if (availableHos.compareTo(requiredTime) < 0) {
            long deficitMinutes = requiredTime.minus(availableHos).toMinutes();
            return Optional.of(new ConstraintViolation(
                    ID,
                    NAME,
                    ConstraintSeverity.HARD,
                    String.format("Driver '%s' has %d min remaining HOS, but route requires %d min (deficit: %d min)",
                            candidate.driver().name(), availableHos.toMinutes(), requiredTime.toMinutes(), deficitMinutes),
                    Map.of(
                            "driverId", candidate.driver().driverId().toString(),
                            "remainingHosMin", availableHos.toMinutes(),
                            "requiredHosMin", requiredTime.toMinutes(),
                            "deficitMin", deficitMinutes
                    ),
                    context.timestamp()
            ));
        }

        return Optional.empty();
    }
}
