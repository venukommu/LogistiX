package org.logistix.engine.steps;

/**
 * Pipeline step contract responsible for AI/LLM model invocation, semantic reasoning, and contextual inference.
 */
public interface AIStep extends DecisionStep {

    default String getStepType() {
        return "AI_STEP";
    }
}
