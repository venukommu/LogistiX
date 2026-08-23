package org.logistix.examples.dispatch.constraints;

import org.logistix.domain.constraint.Constraint;
import org.logistix.domain.constraint.ConstraintSeverity;
import org.logistix.domain.constraint.ConstraintViolation;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.examples.dispatch.model.Certification;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Hard constraint ensuring the driver holds all special endorsements or certifications
 * mandated by the shipment cargo (e.g. HAZMAT, REEFER, OVERSIZED, TWIC).
 */
public class DriverCertificationConstraint implements Constraint<DispatchCandidate> {

    public static final String ID = "CONSTRAINT_CERTIFICATIONS";
    public static final String NAME = "Mandatory Cargo Certifications";

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
        Set<Certification> required = candidate.shipment().requiredCertifications();
        if (required.isEmpty()) {
            return Optional.empty();
        }

        Set<Certification> held = candidate.driver().certifications();
        if (!held.containsAll(required)) {
            Set<Certification> missing = new HashSet<>(required);
            missing.removeAll(held);

            return Optional.of(new ConstraintViolation(
                    ID,
                    NAME,
                    ConstraintSeverity.HARD,
                    String.format("Driver '%s' is missing required certifications: %s", candidate.driver().name(), missing),
                    Map.of(
                            "driverId", candidate.driver().driverId().toString(),
                            "heldCertifications", held.toString(),
                            "requiredCertifications", required.toString(),
                            "missingCertifications", missing.toString()
                    ),
                    context.timestamp()
            ));
        }

        return Optional.empty();
    }
}
