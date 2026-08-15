package org.logistix.engine.steps;

/**
 * Pipeline step contract responsible for evaluating operational feasibility and hard constraint pruning.
 */
public interface ConstraintStep extends DecisionStep {

    default String getStepType() {
        return "CONSTRAINT_STEP";
    }
}
