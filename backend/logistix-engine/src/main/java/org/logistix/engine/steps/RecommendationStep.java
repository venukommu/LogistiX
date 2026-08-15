package org.logistix.engine.steps;

/**
 * Pipeline step contract responsible for synthesizing candidate scores into ranked recommendations with explainability.
 */
public interface RecommendationStep extends DecisionStep {

    default String getStepType() {
        return "RECOMMENDATION_STEP";
    }
}
