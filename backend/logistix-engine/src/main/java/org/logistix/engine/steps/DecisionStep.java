package org.logistix.engine.steps;

import org.logistix.domain.decision.DecisionContext;

/**
 * Primary unit of execution within a DecisionPipeline.
 * Transforms an input DecisionContext into an updated DecisionContext wrapped in a StepResult.
 */
public interface DecisionStep {

    StepMetadata getMetadata();

    StepResult execute(DecisionContext context);
}
