package org.logistix.engine.steps;

/**
 * Pipeline step contract responsible for multi-criteria weighting, normalized scoring, and confidence computation.
 */
public interface ScoringStep extends DecisionStep {

    default String getStepType() {
        return "SCORING_STEP";
    }
}
