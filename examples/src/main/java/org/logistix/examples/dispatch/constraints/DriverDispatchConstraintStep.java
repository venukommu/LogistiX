package org.logistix.examples.dispatch.constraints;

import org.logistix.domain.constraint.Constraint;
import org.logistix.domain.constraint.ConstraintViolation;
import org.logistix.domain.decision.DecisionContext;
import org.logistix.domain.fact.Fact;
import org.logistix.engine.steps.ConstraintStep;
import org.logistix.engine.steps.StepMetadata;
import org.logistix.engine.steps.StepResult;
import org.logistix.examples.dispatch.model.DispatchCandidate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Pipeline step executing all operational feasibility constraints for Driver Dispatch.
 */
public class DriverDispatchConstraintStep implements ConstraintStep {

    public static final String STEP_ID = "step-dispatch-constraints";
    public static final String STEP_NAME = "Driver Dispatch Hard Constraints Filter";

    private final List<Constraint<DispatchCandidate>> constraints;

    public DriverDispatchConstraintStep() {
        this(List.of(
                new HoursOfServiceConstraint(),
                new VehicleCapacityConstraint(),
                new DriverCertificationConstraint(),
                new DeliveryDeadlineConstraint()
        ));
    }

    public DriverDispatchConstraintStep(List<Constraint<DispatchCandidate>> constraints) {
        this.constraints = List.copyOf(constraints);
    }

    @Override
    public StepMetadata getMetadata() {
        return StepMetadata.of(STEP_ID, STEP_NAME, 10);
    }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult execute(DecisionContext context) {
        Instant start = Instant.now();

        List<DispatchCandidate> rawCandidates = context.getFactValue("candidates", List.class)
                .orElse(Collections.emptyList());

        List<DispatchCandidate> feasibleCandidates = new ArrayList<>();
        List<ConstraintViolation> violations = new ArrayList<>();

        for (DispatchCandidate candidate : rawCandidates) {
            boolean isFeasible = true;
            for (Constraint<DispatchCandidate> constraint : constraints) {
                Optional<ConstraintViolation> violation = constraint.evaluate(candidate, context);
                if (violation.isPresent()) {
                    violations.add(violation.get());
                    isFeasible = false;
                    break; // Prune immediately on first hard violation
                }
            }
            if (isFeasible) {
                feasibleCandidates.add(candidate);
            }
        }

        Duration duration = Duration.between(start, Instant.now());

        DecisionContext updatedContext = context
                .withFact(Fact.of("feasibleCandidates", feasibleCandidates))
                .withFact(Fact.of("violatedConstraints", violations));

        if (feasibleCandidates.isEmpty()) {
            return StepResult.success(
                    updatedContext,
                    duration,
                    List.of(Fact.of("feasibleCandidates", feasibleCandidates)),
                    String.format("Constraints evaluated: 0 of %d candidates are feasible (%d violations recorded)",
                            rawCandidates.size(), violations.size())
            );
        }

        return StepResult.success(
                updatedContext,
                duration,
                List.of(Fact.of("feasibleCandidates", feasibleCandidates)),
                String.format("Constraints evaluated: %d of %d candidates are feasible (%d pruned)",
                        feasibleCandidates.size(), rawCandidates.size(), violations.size())
        );
    }
}
